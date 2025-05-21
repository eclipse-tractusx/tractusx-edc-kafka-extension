package org.eclipse.tractusx.edc.local.services;

import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.keys.keyparsers.PemParser;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import java.security.PublicKey;

public class VaultPublicKeyResolver implements DidPublicKeyResolver {

  private final Vault vault;
  private final PemParser pemParser;

  public VaultPublicKeyResolver(final Vault vault, final PemParser pemParser) {
    this.vault = vault;
    this.pemParser = pemParser;
  }

  @Override
  public Result<PublicKey> resolveKey(final String id) {
    var encoded = vault.resolveSecret("public-key");
    var key = pemParser.parse(encoded);
    if (key instanceof PublicKey publicKey) {
      return Result.success(publicKey);
    }

    return Result.failure("The specified key is not a public key.");
  }
}
