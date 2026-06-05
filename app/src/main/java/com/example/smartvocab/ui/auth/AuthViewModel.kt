package com.example.smartvocab.ui.auth

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartvocab.data.EmailSender
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlin.random.Random

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    // OTP
    val generatedOtp: String = "",
    val otpSentTime: Long = 0L,
    val otpExpirySeconds: Int = 300, // 5 minutes
    val isOtpSent: Boolean = false,
    val isOtpVerified: Boolean = false,
    // Pending registration data
    val pendingFullName: String = "",
    val pendingEmail: String = "",
    val pendingPassword: String = "",
    // Forgot password
    val forgotPasswordEmail: String = "",
    // Auth purpose
    val authPurpose: AuthPurpose = AuthPurpose.REGISTER
)

enum class AuthPurpose {
    REGISTER,
    FORGOT_PASSWORD
}

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _uiState = mutableStateOf(AuthUiState())
    val uiState: State<AuthUiState> = _uiState

    // Check if user is logged in
    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    // Clear messages
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    fun resetState() {
        _uiState.value = AuthUiState()
    }

    // ==================== LOGIN ====================
    fun loginWithEmail(
        email: String,
        password: String,
        onSuccess: () -> Unit
    ) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Vui lòng nhập đầy đủ thông tin")
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                _uiState.value = _uiState.value.copy(isLoading = false)
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    val msg = when {
                        task.exception?.message?.contains("password", true) == true -> "Mật khẩu không đúng"
                        task.exception?.message?.contains("no user", true) == true -> "Tài khoản không tồn tại"
                        task.exception?.message?.contains("badly formatted", true) == true -> "Email không hợp lệ"
                        else -> "Đăng nhập thất bại: ${task.exception?.localizedMessage}"
                    }
                    _uiState.value = _uiState.value.copy(errorMessage = msg)
                }
            }
    }

    // ==================== GOOGLE SIGN-IN ====================
    fun signInWithGoogle(
        idToken: String,
        onSuccess: () -> Unit
    ) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                _uiState.value = _uiState.value.copy(isLoading = false)
                if (task.isSuccessful) {
                    // Save user profile to Firestore if new user
                    val user = auth.currentUser
                    if (user != null) {
                        val userDoc = firestore.collection("users").document(user.uid)
                        userDoc.get().addOnSuccessListener { doc ->
                            if (!doc.exists()) {
                                val profile = hashMapOf(
                                    "fullName" to (user.displayName ?: ""),
                                    "email" to (user.email ?: ""),
                                    "photoUrl" to (user.photoUrl?.toString() ?: ""),
                                    "createdAt" to System.currentTimeMillis()
                                )
                                userDoc.set(profile)
                            }
                        }
                    }
                    onSuccess()
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Đăng nhập Google thất bại: ${task.exception?.localizedMessage}"
                    )
                }
            }
    }

    // ==================== REGISTER - Step 1: Send OTP ====================
    fun startRegistration(
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String,
        onOtpSent: () -> Unit
    ) {
        // Validate
        if (fullName.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Vui lòng nhập đầy đủ thông tin")
            return
        }
        if (password.length < 6) {
            _uiState.value = _uiState.value.copy(errorMessage = "Mật khẩu phải có ít nhất 6 ký tự")
            return
        }
        if (password != confirmPassword) {
            _uiState.value = _uiState.value.copy(errorMessage = "Mật khẩu xác nhận không khớp")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Email không hợp lệ")
            return
        }

        // Save pending data and send OTP
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            pendingFullName = fullName.trim(),
            pendingEmail = email.trim(),
            pendingPassword = password,
            authPurpose = AuthPurpose.REGISTER
        )

        val otp = generateOtp()
        viewModelScope.launch {
            val sent = EmailSender.sendOtpEmail(email.trim(), otp)
            if (sent) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    generatedOtp = otp,
                    otpSentTime = System.currentTimeMillis(),
                    isOtpSent = true
                )
                onOtpSent()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Không thể gửi mã OTP. Vui lòng thử lại."
                )
            }
        }
    }

    // ==================== REGISTER - Step 2: Verify OTP ====================
    fun verifyOtp(
        inputOtp: String,
        onSuccess: () -> Unit
    ) {
        val state = _uiState.value
        if (inputOtp.length != 6) {
            _uiState.value = state.copy(errorMessage = "Vui lòng nhập đủ 6 chữ số")
            return
        }
        // Check expiry (5 minutes)
        val elapsed = (System.currentTimeMillis() - state.otpSentTime) / 1000
        if (elapsed > state.otpExpirySeconds) {
            _uiState.value = state.copy(errorMessage = "Mã OTP đã hết hạn. Vui lòng gửi lại.")
            return
        }
        if (inputOtp != state.generatedOtp) {
            _uiState.value = state.copy(errorMessage = "Mã OTP không đúng")
            return
        }

        _uiState.value = state.copy(isOtpVerified = true, errorMessage = null)

        // If register purpose, complete registration
        if (state.authPurpose == AuthPurpose.REGISTER) {
            completeRegistration(onSuccess)
        } else {
            // Forgot password - send Firebase reset email
            completeForgotPassword(onSuccess)
        }
    }

    // ==================== REGISTER - Step 3: Create account ====================
    private fun completeRegistration(onSuccess: () -> Unit) {
        val state = _uiState.value
        _uiState.value = state.copy(isLoading = true)

        auth.createUserWithEmailAndPassword(state.pendingEmail, state.pendingPassword)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        // Save user profile to Firestore
                        val profile = hashMapOf(
                            "fullName" to state.pendingFullName,
                            "email" to state.pendingEmail,
                            "createdAt" to System.currentTimeMillis()
                        )
                        firestore.collection("users").document(user.uid)
                            .set(profile)
                            .addOnCompleteListener {
                                // Sign out after registration (user needs to login)
                                auth.signOut()
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    successMessage = "Đăng ký thành công! Vui lòng đăng nhập."
                                )
                                onSuccess()
                            }
                            .addOnFailureListener { e ->
                                auth.signOut()
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    errorMessage = "Tạo hồ sơ thất bại: ${e.message}"
                                )
                            }
                    }
                } else {
                    val msg = when {
                        task.exception?.message?.contains("already in use", true) == true -> "Email này đã được sử dụng"
                        task.exception?.message?.contains("weak password", true) == true -> "Mật khẩu quá yếu"
                        else -> "Đăng ký thất bại: ${task.exception?.localizedMessage}"
                    }
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = msg)
                }
            }
    }

    // ==================== FORGOT PASSWORD - Step 1: Send OTP ====================
    fun sendForgotPasswordOtp(
        email: String,
        onOtpSent: () -> Unit
    ) {
        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Vui lòng nhập email")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Email không hợp lệ")
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            forgotPasswordEmail = email.trim(),
            authPurpose = AuthPurpose.FORGOT_PASSWORD
        )

        val otp = generateOtp()
        viewModelScope.launch {
            val sent = EmailSender.sendOtpEmail(email.trim(), otp)
            if (sent) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    generatedOtp = otp,
                    otpSentTime = System.currentTimeMillis(),
                    isOtpSent = true
                )
                onOtpSent()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Không thể gửi mã OTP. Vui lòng thử lại."
                )
            }
        }
    }

    // ==================== FORGOT PASSWORD - Step 2: After OTP verified ====================
    private fun completeForgotPassword(onSuccess: () -> Unit) {
        val state = _uiState.value
        _uiState.value = state.copy(isLoading = true)

        auth.sendPasswordResetEmail(state.forgotPasswordEmail)
            .addOnCompleteListener { task ->
                _uiState.value = _uiState.value.copy(isLoading = false)
                if (task.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Link đặt lại mật khẩu đã được gửi đến email của bạn. Vui lòng kiểm tra email."
                    )
                    onSuccess()
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Không thể gửi link đặt lại mật khẩu: ${task.exception?.localizedMessage}"
                    )
                }
            }
    }

    // ==================== RESEND OTP ====================
    fun resendOtp(onSent: () -> Unit) {
        val state = _uiState.value
        val email = if (state.authPurpose == AuthPurpose.REGISTER) state.pendingEmail else state.forgotPasswordEmail
        if (email.isBlank()) return

        _uiState.value = state.copy(isLoading = true, errorMessage = null)
        val otp = generateOtp()
        viewModelScope.launch {
            val sent = EmailSender.sendOtpEmail(email, otp)
            if (sent) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    generatedOtp = otp,
                    otpSentTime = System.currentTimeMillis()
                )
                onSent()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Không thể gửi lại mã OTP. Vui lòng thử lại."
                )
            }
        }
    }

    // ==================== SIGN OUT ====================
    fun signOut() {
        auth.signOut()
    }

    // ==================== HELPERS ====================
    private fun generateOtp(): String {
        return String.format("%06d", Random.nextInt(0, 1000000))
    }

    fun getOtpRemainingSeconds(): Int {
        val state = _uiState.value
        if (state.otpSentTime == 0L) return 0
        val elapsed = (System.currentTimeMillis() - state.otpSentTime) / 1000
        return maxOf(0, (state.otpExpirySeconds - elapsed).toInt())
    }
}
