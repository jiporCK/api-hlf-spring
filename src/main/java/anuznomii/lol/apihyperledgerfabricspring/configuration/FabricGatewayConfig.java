package anuznomii.lol.apihyperledgerfabricspring.configuration;

import anuznomii.lol.apihyperledgerfabricspring.services.FabricCAService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FabricGatewayConfig {

    private final FabricCAService fabricCAService;

    @Value("${fabric.wallet.config-path}")
    private String walletPath;
    @Value("${fabric.network.config-path}")
    private String networkConfigPath;

    @Value("${fabric.network.discovery}")
    private Boolean isDiscovered;

    @Bean
    public Gateway createGateway() throws Exception {

        Wallet wallet = Wallets.newFileSystemWallet(
                Paths.get(walletPath)
        );
        var networkConfig = Paths.get(networkConfigPath);
        // create gateway
        Gateway.Builder builder = Gateway.createBuilder()
                .identity(wallet, "admin")
                .networkConfig(networkConfig)
                .discovery(isDiscovered)
                .commitTimeout(60, TimeUnit.SECONDS);



        var gateway = builder.connect();
        log.info("Gateway connection is successful");
        return gateway;
    }


}
