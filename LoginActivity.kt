package sk.drevari.optitimb.ui.login

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.afollestad.materialdialogs.MaterialDialog
import kotlinx.android.synthetic.main.activity_login.*
import sk.drevari.optitimb.R
import sk.drevari.optitimb.databinding.ActivityLoginBinding
import sk.drevari.optitimb.ui.MainActivity


class LoginActivity : AppCompatActivity() {

    private val loginViewModel by viewModels<LoginViewModel>()
    private lateinit var binding: ActivityLoginBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (loginViewModel.isUserLoggedIn()) startMainActivity()

        loginViewModel.loginFormState.observe(this, Observer {
            val loginState = it ?: return@Observer

            // disable login button unless both username / password is valid
            binding.edUserName.isEnabled = loginState.isDataValid
            binding.btnLogin.isEnabled = loginState.isDataValid

            if (loginState.usernameError != null) {
                binding.edUserName.error = getString(loginState.usernameError)
            }
            if (loginState.passwordError != null) {
                binding.edPassword.error = getString(loginState.passwordError)
            }
        })

        loginViewModel.loginResult.observe(this, Observer {
            val loginResult = it ?: return@Observer

            loading.visibility = View.GONE
            if (loginResult.error != null) {
                showLoginFailed(loginResult.error)

            } else {
                if (loginResult.success != null) {
                    updateUiWithUser(loginResult.success)
                }
                setResult(Activity.RESULT_OK)

                startMainActivity()
            }
        })

        binding.edUserName.afterTextChanged {
            loginViewModel.loginDataChanged(
                binding.edUserName.text.toString(),
                binding.edPassword.text.toString()
            )
        }

        binding.edPassword.apply {
            afterTextChanged {
                loginViewModel.loginDataChanged(
                    binding.edUserName.text.toString(),
                    binding.edPassword.text.toString()
                )
            }

            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE ->
                        onClick_btnLogin(binding.btnLogin)
                }
                false
            }
        }
        binding.btnLogin.setOnClickListener(::onClick_btnLogin)
        binding.llMoreInfo.setOnClickListener(::onClick_llMoreInfo)
        binding.llRegistration.setOnClickListener(::onClick_llRegistration)

    }

    private fun startMainActivity() {
        val mainIntent = Intent(this, MainActivity::class.java)
        startActivity(mainIntent)
        finish()
    }
    fun onClick_llRegistration(view2: View) {
        MaterialDialog(this)
            .title(R.string.MORE_INFO)
            .message(R.string.msgMoreInfo)
            .positiveButton(R.string.btnClose)
            .show()
    }

    fun onClick_btnLogin(view: View) {
        binding.loading.visibility = View.VISIBLE
        loginViewModel.login(binding.edUserName.text.toString(), binding.edPassword.text.toString())
    }

    fun onClick_llMoreInfo(view2: View) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.moreInfoURL))))
    }

    private fun updateUiWithUser(model: LoggedInUserView) {
        val welcome = getString(R.string.lblWelcome)
        val displayName = model.displayName
        Toast.makeText(
            applicationContext,
            "$welcome $displayName",
            Toast.LENGTH_LONG
        ).show()
        openMainWindow()
    }

    private fun openMainWindow() {
        val mainIntent = Intent(this, MainActivity::class.java)
        startActivity(mainIntent)
        finish()
    }

    private fun showLoginFailed(@StringRes errorString: Int) {
        Toast.makeText(applicationContext, errorString, Toast.LENGTH_SHORT).show()
    }
}


fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}
