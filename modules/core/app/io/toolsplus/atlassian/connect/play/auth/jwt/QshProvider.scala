package io.toolsplus.atlassian.connect.play.auth.jwt

import io.toolsplus.atlassian.jwt.HttpRequestCanonicalizer
import io.toolsplus.atlassian.jwt.api.CanonicalHttpRequest

/**
  * Base trait for Query string hash providers
  */
sealed trait QshProvider

/**
  * Query string hash provider that returns a static value of "context-qsh" for the query string hash.
  */
case object ContextQshProvider extends QshProvider {
  def qsh: String = "context-qsh"
}

/**
  * Query string hash provider that computes the QSH from the given canonical HTTP request representation.
  */
case object CanonicalHttpRequestQshProvider extends QshProvider {
  def qsh(canonicalHttpRequest: CanonicalHttpRequest): String = HttpRequestCanonicalizer.computeCanonicalRequestHash(
    canonicalHttpRequest)
}
