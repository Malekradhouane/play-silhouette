package utils

import models.User
import com.mohiva.play.silhouette.api.Env
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator

/**
 * The default env.
 */
trait MyEnv extends Env {
  type I = User
  type A = JWTAuthenticator
}