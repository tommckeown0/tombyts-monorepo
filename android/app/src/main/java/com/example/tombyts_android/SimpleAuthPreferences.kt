import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class SimpleAuthPreferences(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    fun saveAuthToken(token: String) {
        Log.d("Auth", "Saving auth token: ${token.take(20)}... (length: ${token.length})")
        sharedPreferences.edit()
            .putString("authToken", token)
            .apply()
    }

    fun getAuthToken(): String? {
        val token = sharedPreferences.getString("authToken", null)
        if (token != null) {
            Log.d("Auth", "Retrieved stored token: ${token.take(20)}... (length: ${token.length})")
        } else {
            Log.d("Auth", "No stored token found")
        }
        return token
    }

    fun saveUsername(username: String) {
        sharedPreferences.edit()
            .putString("username", username)
            .apply()
    }

    fun getUsername(): String? {
        return sharedPreferences.getString("username", null)
    }

    fun clearAuthData() {
        Log.w("Auth", "Clearing auth data (token and username)")
        sharedPreferences.edit()
            .remove("authToken")
            .remove("username")
            .apply()
    }
}