package com.example.smartvocab.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Welcome : Screen("welcome")
    object Goal : Screen("goal")
    object Level : Screen("level")
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")
    object OtpVerification : Screen("otp_verification")
    object Main : Screen("main") // Main App with Bottom Navigation Bar

    // Sub-screens
    object VocabSetDetail : Screen("vocab_set_detail/{setId}") {
        fun createRoute(setId: String) = "vocab_set_detail/$setId"
    }

    object AddEditSet : Screen("add_edit_set?setId={setId}") {
        fun createRoute(setId: String? = null): String {
            val sId = setId ?: "null"
            return "add_edit_set?setId=$sId"
        }
    }
    
    object AddEditWord : Screen("add_edit_word?setId={setId}&wordId={wordId}") {
        fun createRoute(setId: String? = null, wordId: String? = null): String {
            val sId = setId ?: "null"
            val wId = wordId ?: "null"
            return "add_edit_word?setId=$sId&wordId=$wId"
        }
    }
    
    object FlashcardLearning : Screen("learning?setId={setId}&mode={mode}") {
        fun createRoute(setId: String? = null, mode: String? = null): String {
            val sId = setId ?: "null"
            val m = mode ?: "null"
            return "learning?setId=$sId&mode=$m"
        }
    }
    
    object QuizQuestion : Screen("quiz?setId={setId}") {
        fun createRoute(setId: String? = null): String {
            val sId = setId ?: "null"
            return "quiz?setId=$sId"
        }
    }
    
    object QuizResult : Screen("quiz_result?score={score}&total={total}&time={time}&setId={setId}") {
        fun createRoute(score: Int, total: Int, time: String, setId: String? = null): String {
            val sId = setId ?: "null"
            return "quiz_result?score=$score&total=$total&time=$time&setId=$sId"
        }
    }
    
    object Notification : Screen("notifications")
}
