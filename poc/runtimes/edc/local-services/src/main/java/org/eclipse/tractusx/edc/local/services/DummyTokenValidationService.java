package org.eclipse.tractusx.edc.local.services;

import org.eclipse.edc.keys.spi.PublicKeyResolver;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.eclipse.edc.token.spi.TokenValidationService;
import java.util.List;

public class DummyTokenValidationService implements TokenValidationService {

  @Override
  public Result<ClaimToken> validate(final TokenRepresentation tokenRepresentation, final PublicKeyResolver publicKeyResolver, final List<TokenValidationRule> list) {
    var claimToken = ClaimToken.Builder.newInstance()
      .claim("valid", "true")
      .build();
    return Result.success(claimToken);
  }

}
