package object_oriented_analysis_design.authenticator

import java.security.SecureRandom
import java.util
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import scala.util.Try

object HashApi {
  private val rand = new SecureRandom()
  private val iterations = 10000
  private val keyLength = 256

  /**
   * Returns a random salt to be used to hash a password.
   *
   * @return a 16 bytes random salt
   */
  def generateSalt: Array[Byte] = {
    val salt = new Array[Byte](16)
    rand.nextBytes(salt)
    salt
  }

  /**
   * Returns a salted and hashed password using the provided hash.
   *
   * @param password the password to be hashed
   * @param salt     a 16 bytes salt
   * @return the hashed password with salt
   */
  def generateHash(password: Array[Char], salt: Array[Byte]): Array[Byte] = {
    val spec = new PBEKeySpec(password, salt, iterations, keyLength)
    util.Arrays.fill(password, Character.MIN_VALUE)
    Try {
      val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
      skf.generateSecret(spec).getEncoded
    }.recover {
      case ex: Exception =>
        spec.clearPassword()
        sys.error("Error while hashing a password: " + ex.getMessage)
    }.get
  }

  /**
   * Returns true if the given password and salt match the hashed value, false otherwise.
   *
   * @param password     the password to check
   * @param salt         the salt used to hash the password
   * @param expectedHash the expected hashed value of the password
   * @return true if the given password and salt match the hashed value, false otherwise
   */
  def hashIsValid(password: Array[Char], salt: Array[Byte], expectedHash: Array[Byte]): Boolean = {
    val pwdHash = generateHash(password, salt)
    util.Arrays.fill(password, Character.MIN_VALUE)
    if (pwdHash.length != expectedHash.length) return false
    if (!(pwdHash sameElements expectedHash)) false
    else true
  }
}
