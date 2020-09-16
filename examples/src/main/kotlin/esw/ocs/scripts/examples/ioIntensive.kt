package esw.ocs.scripts.examples
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader

//#io-bound-function
suspend fun BufferedReader.readMessage(): CharSequence? {
  return withContext(Dispatchers.IO) {
    readLine()
  }
}
//#io-bound-function
