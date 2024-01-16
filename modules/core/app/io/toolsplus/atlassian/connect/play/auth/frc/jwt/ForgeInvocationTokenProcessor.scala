package io.toolsplus.atlassian.connect.play.auth.frc.jwt

import com.nimbusds.jwt.proc.{
  DefaultJWTClaimsVerifier,
  DefaultJWTProcessor,
  JWTProcessor
}
import com.nimbusds.jwt.{JWTClaimNames, JWTClaimsSet}

import java.util

object ForgeInvocationTokenProcessor {

  /**
    * Creates an JWT processor that validates Forge Invocation Tokens (FIT).
    *
    * @param appId ID of the Forge app associated with the FITs
    * @param keySelector JWK selector for Forge Remote requests
    * @return JWT processor that validates Forge Invocation Tokens
    */
  def create(appId: String, keySelector: ForgeJWSVerificationKeySelector)
    : JWTProcessor[ForgeInvocationContext] = {
    val processor = new DefaultJWTProcessor[ForgeInvocationContext]()
    processor.setJWSKeySelector(keySelector)
    processor.setJWTClaimsSetVerifier(
      new DefaultJWTClaimsVerifier(
        new JWTClaimsSet.Builder()
          .issuer("forge/invocation-token")
          .audience(appId)
          .build(),
        new util.HashSet(
          util.Arrays.asList(JWTClaimNames.ISSUED_AT,
                             JWTClaimNames.EXPIRATION_TIME,
                             JWTClaimNames.NOT_BEFORE,
                             JWTClaimNames.JWT_ID))
      ))
    processor
  }
}
