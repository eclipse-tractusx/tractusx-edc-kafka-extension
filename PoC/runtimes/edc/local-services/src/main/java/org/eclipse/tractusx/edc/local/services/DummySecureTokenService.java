package org.eclipse.tractusx.edc.local.services;

import org.eclipse.edc.iam.identitytrust.spi.SecureTokenService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.Nullable;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

public class DummySecureTokenService implements SecureTokenService {

  @Override
  public Result<TokenRepresentation> createToken(Map<String, String> map, @Nullable String s) {
    var tokenRepresentation = TokenRepresentation.Builder.newInstance()
      .token("token")
      .expiresIn(LocalDateTime.of(2030, 1, 1, 1, 1).toEpochSecond(ZoneOffset.UTC))
      .build();

    return Result.success(tokenRepresentation);
  }
}
