package org.eclipse.tractusx.edc.local.services;

import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;

public class DummyBdrsClient implements BdrsClient {
  @Override
  public String resolve(final String bpn) {
    return "did:web:" + bpn;
  }
}
