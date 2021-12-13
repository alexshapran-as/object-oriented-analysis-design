package object_oriented_analysis_design.authenticator

object AuthApi {
    def authorize(username: String, password: String): Option[List[String]] = {
        UserAuthData.find(username) match {
            case Some(UserAuthData(_, passwordHash, salt, roles))
                if HashApi.hashIsValid(password.toCharArray, salt, passwordHash) => Some(roles.map(_.toString))
            case _ => None
        }
    }

    private val passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*_=+-]).{8,}$".r

    def register(username: String, password: String): Option[String] = {
        val passwordErrMsg = password match {
            case passwordPattern() => None
            case _ => Some("""Password must contain: |1. at least one number; |2. at least one uppercase and lowercase letter;
                  ||3. at least 8 or more characters; |4. at least one special symbol (!@#$%^&*=+-_)""".stripMargin)
        }
        val usernameErrMsg = if (username.trim.isEmpty) Some("Username can not be empty") else None
        if (passwordErrMsg.isEmpty && usernameErrMsg.isEmpty) {
            val salt = HashApi.generateSalt
            val userWasCreated = UserAuthData(
                username, HashApi.generateHash(password.toCharArray, salt), salt, List(Roles.ADMIN)
            ).save
            if (userWasCreated) None else Some("User with the same username already exists")
        } else {
            Some(passwordErrMsg.getOrElse(usernameErrMsg.get))
        }
    }

    def changePassword(username: String, oldPassword: String, newPassword: String): Option[String] = {
        UserAuthData.find(username) match {
            case None => Some("Username invalid")
            case Some(user @ UserAuthData(_, passwordHash, salt, _)) =>
                if (!HashApi.hashIsValid(oldPassword.toCharArray, salt, passwordHash)) Some("Old password invalid")
                else {
                    val newSalt = HashApi.generateSalt
                    val newPasswordHash = HashApi.generateHash(newPassword.toCharArray, newSalt)
                    user.copy(passwordHash = newPasswordHash, salt = newSalt).save
                    None
                }
        }
    }

    def hasAccessToWithRoles(pathPrefix: String, roles: List[String]): Boolean =
        if (roles.contains(Roles.ADMIN.toString)) true else roles.contains(pathPrefix.toUpperCase)
}

