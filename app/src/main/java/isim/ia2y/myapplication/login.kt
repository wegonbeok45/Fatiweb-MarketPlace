package isim.ia2y.myapplication

import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class login : AppCompatActivity() {
    private var passwordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setupLoginActions()
        revealViewsInOrder(
            R.id.ivBack,
            R.id.ivAppLogo,
            R.id.tvWelcomeTitle,
            R.id.tvWelcomeSubtitle,
            R.id.cardEmailField,
            R.id.cardPasswordField,
            R.id.btnLogin,
            R.id.layoutSocialButtons,
            R.id.layoutSignUpRow
        )
        emphasizeCta(R.id.btnLogin)
    }

    private fun setupLoginActions() {
        findViewById<View>(R.id.ivBack)?.setOnClickListener {
            navigateToMainTab(MainActivity.Tab.PROFILE)
        }
        findViewById<View>(R.id.ivAppLogo)?.setOnClickListener {
            navigateToMainTab(MainActivity.Tab.HOME)
        }
        findViewById<View>(R.id.tvSignUp)?.setOnClickListener {
            navigateNoShift(register::class.java)
        }

        bindComingSoon(R.id.tvForgotPassword, R.id.btnGoogle, R.id.btnFacebook)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        bindInputFieldMotion(R.id.cardEmailField, R.id.etEmail) { value ->
            value.contains("@") && value.contains(".")
        }
        bindInputFieldMotion(R.id.cardPasswordField, R.id.etPassword) { value ->
            value.length >= 6
        }

        findViewById<View>(R.id.btnLogin)?.setOnClickListener {
            val hasValidEmail = etEmail.text?.toString().orEmpty().contains("@")
            val hasValidPassword = (etPassword.text?.length ?: 0) >= 6
            if (!hasValidEmail || !hasValidPassword) {
                if (!hasValidEmail) markInputState(R.id.cardEmailField, InputFieldState.ERROR)
                if (!hasValidPassword) markInputState(R.id.cardPasswordField, InputFieldState.ERROR)
                showMotionSnackbar(getString(R.string.login_validation_error))
                return@setOnClickListener
            }

            markInputState(R.id.cardEmailField, InputFieldState.SUCCESS)
            markInputState(R.id.cardPasswordField, InputFieldState.SUCCESS)
            showMotionSnackbar(getString(R.string.login_placeholder_success))
            navigateToMainTab(MainActivity.Tab.HOME)
        }

        findViewById<View>(R.id.ivPasswordToggle)?.setOnClickListener {
            passwordVisible = !passwordVisible
            etPassword.transformationMethod =
                if (passwordVisible) null else PasswordTransformationMethod.getInstance()
            etPassword.setSelection(etPassword.text?.length ?: 0)
        }
        etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                findViewById<View>(R.id.btnLogin)?.performClick()
                true
            } else {
                false
            }
        }
        applyPressFeedback(
            R.id.ivBack,
            R.id.ivAppLogo,
            R.id.tvSignUp,
            R.id.tvForgotPassword,
            R.id.btnLogin,
            R.id.btnGoogle,
            R.id.btnFacebook,
            R.id.ivPasswordToggle
        )
    }
}
