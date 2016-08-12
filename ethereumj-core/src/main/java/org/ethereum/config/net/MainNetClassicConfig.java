package org.ethereum.config.net;

import org.ethereum.config.blockchain.FrontierConfig;
import org.ethereum.config.blockchain.HomesteadConfig;

public class MainNetClassicConfig extends AbstractNetConfig {
    public static final MainNetClassicConfig INSTANCE = new MainNetClassicConfig();

    public MainNetClassicConfig() {
        add(0, new FrontierConfig());
        add(1_150_000, new HomesteadConfig());
    }
}
