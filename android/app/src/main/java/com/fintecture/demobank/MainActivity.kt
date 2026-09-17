package com.fintecture.demobank

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.fintecture.demobank.databinding.ActivityMainBinding

/**
 * The Demo Bank's consent screen, as a native app.
 *
 * It exists so an integrator can test the app to app handoff and the return to their own app in
 * sandbox, which is impossible while the simulated bank is only a web page: the operating system is
 * never asked to open anything.
 *
 * It shows no payment details on purpose. The point under test is whether the OS hands off and
 * whether the payer comes back, and nothing about that needs an amount on screen.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val strandViewModel: StrandViewModel by viewModels()
    private var session: DemoBankSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bindPickers()
        binding.confirm.setOnClickListener { confirm() }
        binding.cancel.setOnClickListener { finish() }
        binding.strand.setOnClickListener { strand() }

        // Stranding and cancelling both leave Connect on its loader, which is a bug of ours that is
        // not fixed yet, so a build handed to an integrator offers Confirm only.
        if (!BuildConfig.SHOW_DIAGNOSTIC_ACTIONS) {
            binding.strand.visibility = View.GONE
            binding.strandHint.visibility = View.GONE
            binding.cancel.visibility = View.GONE
        }

        strandViewModel.result.observe(this) { render(it) }
        applySession(DemoBankSession.from(intent?.data))
    }

    /** singleTask means a second handoff arrives here rather than in a new instance. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        strandViewModel.clear()
        applySession(DemoBankSession.from(intent.data))
    }

    private fun applySession(incoming: DemoBankSession?) {
        session = incoming
        val hasSession = incoming != null

        binding.noSession.visibility = if (hasSession) View.GONE else View.VISIBLE
        binding.outcomeCard.visibility = if (hasSession) View.VISIBLE else View.GONE
        binding.status.visibility = View.GONE
        setActionsEnabled(hasSession && !strandViewModel.isRunning)
    }

    private fun setActionsEnabled(enabled: Boolean) {
        binding.confirm.isEnabled = enabled
        binding.strand.isEnabled = enabled
    }

    private fun bindPickers() {
        binding.sessionStatus.adapter = adapterOf(Outcomes.SESSION_STATUSES)
        binding.transferReason.adapter = adapterOf(Outcomes.TRANSFER_REASONS)

        // The transfer states depend on the status, exactly as the web screen does it. An
        // unsupported pair returns no payload from the connector and the callback then fails on a
        // missing render, so the tester must not be able to build one.
        binding.sessionStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val status = Outcomes.SESSION_STATUSES[position]
                val failed = status == DemoBankSession.PAYMENT_UNSUCCESSFUL

                binding.transferState.adapter = adapterOf(Outcomes.transferStatesFor(status))
                binding.transferStateGroup.visibility = if (failed) View.GONE else View.VISIBLE
                binding.transferReasonGroup.visibility = if (failed) View.VISIBLE else View.GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun adapterOf(values: List<String>) =
        ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, values)

    private fun selectedOutcome(): Outcome {
        val status = binding.sessionStatus.selectedItem as String
        return Outcome(
            sessionStatus = status,
            // Read from the list rather than the spinner: the adapter is swapped when the status
            // changes, and a stale selection would be an invalid pair.
            transferState = binding.transferState.selectedItem as? String
                ?: Outcomes.transferStatesFor(status).first(),
            transferReason = binding.transferReason.selectedItem as String,
        )
    }

    /** The ordinary path: hand the callback to the OS, which is what a real bank app does. */
    private fun confirm() {
        val url = session?.callbackUrl(selectedOutcome()) ?: return
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        finish()
    }

    /**
     * Completes the payment without returning the payer, reproducing a bank that takes the money
     * and strands them.
     */
    private fun strand() {
        val current = session ?: return
        setActionsEnabled(false)
        strandViewModel.strand(current, selectedOutcome())
    }

    private fun render(result: StrandViewModel.Result?) {
        // A result belongs to the session that started it; a newer handoff must not inherit it.
        if (result == null || result.stateOrNull() != session?.state) {
            binding.status.visibility = View.GONE
            setActionsEnabled(session != null && !strandViewModel.isRunning)
            return
        }

        binding.status.visibility = View.VISIBLE
        binding.status.text = when (result) {
            is StrandViewModel.Result.CallbackDriven -> getString(R.string.stranded)
            is StrandViewModel.Result.NotApplied -> getString(R.string.strand_not_applied, result.httpStatus)
            is StrandViewModel.Result.Failed -> getString(R.string.strand_failed)
        }
        setActionsEnabled(result !is StrandViewModel.Result.CallbackDriven)
    }

    private fun StrandViewModel.Result.stateOrNull(): String = when (this) {
        is StrandViewModel.Result.CallbackDriven -> state
        is StrandViewModel.Result.NotApplied -> state
        is StrandViewModel.Result.Failed -> state
    }
}
