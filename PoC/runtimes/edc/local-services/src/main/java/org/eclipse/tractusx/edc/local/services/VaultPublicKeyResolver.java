package org.eclipse.tractusx.edc.local.services;

import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.keys.keyparsers.PemParser;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import java.security.PublicKey;

public class VaultPublicKeyResolver implements DidPublicKeyResolver {

  private Vault vault;
  private PemParser pemParser;

  public VaultPublicKeyResolver(Vault vault, PemParser pemParser) {
    this.vault = vault;
    this.pemParser = pemParser;
  }

  @Override
  public Result<PublicKey> resolveKey(String id) {
    var encoded = vault.resolveSecret("public-key");
    var key = pemParser.parse(encoded);
    if (key instanceof PublicKey publicKey) {
      return Result.success(publicKey);
    }

    return Result.failure("The specified key is not a public key.");
  }
}
