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

class register : AppCompatActivity() {
    private var passwordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val hPadding = resources.getDimensionPixelSize(R.dimen.auth_screen_padding)
            val vPadding = resources.getDimensionPixelSize(R.dimen.auth_screen_padding)
            v.setPadding(hPadding, systemBars.top + vPadding, hPadding, systemBars.bottom + vPadding)
            insets
        }
        setupRegisterActions()
        revealViewsInOrder(
            R.id.ivBack,
            R.id.ivAppLogo,
            R.id.tvHeadline,
            R.id.tvSubtitle,
            R.id.cardFullNameField,
            R.id.cardEmailField,
            R.id.cardPasswordField,
            R.id.btnRegister,
            R.id.layoutSocialButtons,
            R.id.layoutLoginRow
        )
        emphasizeCta(R.id.btnRegister)
    }

    private fun setupRegisterActions() {
        findViewById<View>(R.id.ivBack)?.setOnClickListener {
            navigateToMainTab(MainActivity.Tab.PROFILE)
        }
        findViewById<View>(R.id.ivAppLogo)?.setOnClickListener {
            navigateToMainTab(MainActivity.Tab.HOME)
        }
        findViewById<View>(R.id.tvGoToLogin)?.setOnClickListener {
            navigateNoShift(login::class.java)
        }

        bindComingSoon(R.id.btnGoogle, R.id.btnFacebook)

        val etFullName = findViewById<EditText>(R.id.etFullName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        bindInputFieldMotion(R.id.cardFullNameField, R.id.etFullName) { value -> value.length >= 3 }
        bindInputFieldMotion(R.id.cardEmailField, R.id.etEmail) { value ->
            value.contains("@") && value.contains(".")
        }
        bindInputFieldMotion(R.id.cardPasswordField, R.id.etPassword) { value ->
            value.length >= 6
        }

        findViewById<View>(R.id.btnRegister)?.setOnClickListener {
            val hasName = etFullName.text?.toString().orEmpty().trim().length >= 3
            val hasValidEmail = etEmail.text?.toString().orEmpty().contains("@")
            val hasValidPassword = (etPassword.text?.length ?: 0) >= 6
            if (!hasName || !hasValidEmail || !hasValidPassword) {
                if (!hasName) markInputState(R.id.cardFullNameField, InputFieldState.ERROR)
                if (!hasValidEmail) markInputState(R.id.cardEmailField, InputFieldState.ERROR)
                if (!hasValidPassword) markInputState(R.id.cardPasswordField, InputFieldState.ERROR)
                showMotionSnackbar(getString(R.string.register_validation_error))
                return@setOnClickListener
            }

            markInputState(R.id.cardFullNameField, InputFieldState.SUCCESS)
            markInputState(R.id.cardEmailField, InputFieldState.SUCCESS)
            markInputState(R.id.cardPasswordField, InputFieldState.SUCCESS)
            showMotionSnackbar(getString(R.string.register_placeholder_success))
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
                findViewById<View>(R.id.btnRegister)?.performClick()
                true
            } else {
                false
            }
        }
        applyPressFeedback(
            R.id.ivBack,
            R.id.ivAppLogo,
            R.id.tvGoToLogin,
            R.id.btnRegister,
            R.id.btnGoogle,
            R.id.btnFacebook,
            R.id.ivPasswordToggle
        )
    }
}
