package com.nfl.glitr.relay;

public class RelayConfig {

    public static final boolean EXPLICIT_RELAY_NODE_SCAN_DEFAULT = false;
    private final Relay relay;
    private final boolean explicitRelayNodeScanEnabled;


    private RelayConfig(Relay relay, boolean explicitRelayNodeScanEnabled) {
        this.relay = relay;
        this.explicitRelayNodeScanEnabled = explicitRelayNodeScanEnabled;
    }

    public static RelayConfigBuilder newRelayConfig() {
        return new RelayConfigBuilder();
    }

    public static class RelayConfigBuilder {

        private Relay relay = null;
        private boolean explicitRelayNodeScanEnabled = EXPLICIT_RELAY_NODE_SCAN_DEFAULT;

        public RelayConfigBuilder withRelay(Relay relay) {
            this.relay = relay;
            return this;
        }

        public RelayConfigBuilder withExplicitRelayNodeScan() {
            this.explicitRelayNodeScanEnabled = true;
            return this;
        }

        public RelayConfig build() {
            if (relay == null) {
                this.relay = new RelayImpl();
            }

            return new RelayConfig(relay, explicitRelayNodeScanEnabled);
        }
    }

    public Relay getRelay() {
        return relay;
    }

    public boolean isExplicitRelayNodeScanEnabled() {
        return explicitRelayNodeScanEnabled;
    }
}