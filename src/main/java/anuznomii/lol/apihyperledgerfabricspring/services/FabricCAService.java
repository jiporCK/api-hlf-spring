package anuznomii.lol.apihyperledgerfabricspring.services;

import java.io.File;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import anuznomii.lol.apihyperledgerfabricspring.utils.FabricUtils;
import org.hyperledger.fabric.gateway.*;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.EnrollmentRequest;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import anuznomii.lol.apihyperledgerfabricspring.models.CAEnrollmentRequest;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/* 
This service will use used in order to register and enroll the user Fabric CA
 */
@Service
@Slf4j
public class FabricCAService {

    @Value("${fabric.ca.org1.caUrl}")
    private String org1CaUrl;
    @Value("${fabric.ca.org1.certificatePath}")
    private String org1CertificatePath;
    @Value("${fabric.wallet.config-path}")
    private String walletPath;
    private Wallet wallet;

    @Value("${fabric.ca.tls.enabled}")
    private Boolean tlsEnabled;

    // Only for testing
    @Value("${fabric.ca.admin.username}")
    private String adminUsername;
    @Value("${fabric.ca.admin.password}")
    private String adminPassword;

    @PostConstruct
    public void init() throws Exception {
        // Initialize wallet
        this.wallet = Wallets.newFileSystemWallet(Paths.get(walletPath));

        // Check if admin identity exists in the wallet
        if (!FabricUtils.checkIdentityExistence("admin", wallet)) {
            log.info("Admin identity does not exist in wallet. Creating admin identity.");
            createAdminUserOrg1();
            CAEnrollmentRequest request = CAEnrollmentRequest.builder()
                    .username("client1")
                    .affliation("org1.department1")
                    .type("client")
                    .secret("user1pw")
                    .registrarUsername("admin")
                    .build();
            registerAndEnrollAdminUser(request);
        } else {
            log.info("Admin identity already exists in wallet.");
        }
    }


//    @PostConstruct
//    public void init() throws Exception {
//
//        // create wallet instance
//        this.wallet = Wallets.newFileSystemWallet(
//                Paths.get(walletPath));
//
//        // check identity if exists
//        if (FabricUtils.checkIdentityExistence(
//                "admin", wallet
//        )){
//            log.info("Admin already exists in wallet");
//        } else {
//            log.info("Identity already exists in wallet !");
//        }
//        // create admin first, in order to register and enroll the user
//        createAdminUserOrg1();
//
//        // Create a new user
//        CAEnrollmentRequest request = CAEnrollmentRequest.builder()
//                .username("client1")
//                .affliation("org1.department1")
//                .type("client")
//                .secret("user1pw")
//                .registrarUsername("admin")
//                .build();
//        registerAndEnrollUser(request);
//
//    }

    public void registerAndEnrollAdminUser(CAEnrollmentRequest request) throws Exception {

        // HFCA client
        var props = new Properties();
        FabricUtils.setTlsProps(
                props,
                org1CertificatePath,
                tlsEnabled);

        var caClient = HFCAClient.createNewInstance(
                org1CaUrl,
                props);
        caClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());

        // 1. register
        var registrationRequest = new RegistrationRequest(
                request.getUsername());
        registrationRequest.setAffiliation(request.getAffliation());
        registrationRequest.setType("client");
        // we also have the auto generate the secret if the user doens't prvoide us the
        // secret
        registrationRequest.setSecret(request.getSecret());
        registrationRequest.setMaxEnrollments(-1); // unlimited enrollments\

        var adminIdentity = wallet.get("admin");
        if(adminIdentity == null) {
            throw new IllegalAccessException(
                    "Admin identity not found in the wallet"
            );
        }

        // 1.1 getting the registrar
        var adminUser = new User() {

            final X509Identity x509Identity = (X509Identity) adminIdentity;

            @Override
            public String getName() {
                return "admin";
            }

            @Override
            public Set<String> getRoles() {
                return null;
            }

            @Override
            public String getAccount() {
                return null;
            }

            @Override
            public String getAffiliation() {
                return request.getAffliation();
            }

            @Override
            public Enrollment getEnrollment() {
                return new Enrollment() {

                    @Override
                    public PrivateKey getKey() {
                        return x509Identity.getPrivateKey();
                    }

                    @Override
                    public String getCert() {
                        return Identities.toPemString(x509Identity.getCertificate());
                    }

                };
            }

            @Override
            public String getMspId() {
                return "Org1MSP";
            }

        };
        // register the user
        caClient.register(registrationRequest,
                adminUser);
        log.info("Successfully Register a new User :{}", request.getUsername());
        // 2. enroll
        var enrollmentRequest = new EnrollmentRequest();
        enrollmentRequest.setProfile("tls");
        enrollmentRequest.addHost("localhost");

        var enrollment = caClient.enroll(
                request.getUsername(),
                request.getSecret(),
                enrollmentRequest);
        // 3. add to wallet

        var certificate = Identities.readX509Certificate(
                enrollment.getCert());
        var userIdentity = Identities.newX509Identity(
                "Org1MSP", certificate, enrollment.getKey());

        wallet.put("client1", userIdentity);

    }

    // private void storeIdentityToWallet(String label , )
    private void createAdminUserOrg1() throws Exception {

        // 1. create enrollment request
        var enrollmentRequest = new EnrollmentRequest();
        // enrollmentRequest.addAttrReq(); // used in future for attributes
        enrollmentRequest.setProfile("tls");
        enrollmentRequest.addHost("localhost");

        // 2. Setup HFCAClient
        var props = new Properties();
        if (tlsEnabled) {
            // we will configure the tls properties for the HFCA client
            // 2.1. configure tls
            File pemFile = new File(org1CertificatePath);
            if (!pemFile.exists()) {
                throw new Exception("Certificate org1 CA file does not exist");
            }
            // verify the type of the certificate && validity of the certificate
            props.setProperty("pemFile", org1CertificatePath);
            props.setProperty("allowAllHostNames", "true");
            // 2.2. configure the timeout
            props.put("connectTimeout", "30000");
            props.put("readTimeout", "30000");
        }
        var caClient = HFCAClient.createNewInstance(
                org1CaUrl,
                props);
        caClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());

        var enrollment = caClient.enroll(
                adminUsername,
                adminPassword,
                enrollmentRequest);

        var certificate = Identities.readX509Certificate(
                enrollment.getCert());
        var adminIdentity = Identities.newX509Identity(
                "Org1MSP", certificate, enrollment.getKey());

        wallet.put("admin", adminIdentity);
        log.info("Successfully store the identity to the wallet !  ");
    }

    public void registerAndEnrollUser(CAEnrollmentRequest request) throws Exception {

        // HFCA client
        var props = new Properties();
        FabricUtils.setTlsProps(props, org1CertificatePath, tlsEnabled);

        var caClient = HFCAClient.createNewInstance(org1CaUrl, props);
        caClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());

        // 1. register
        var registrationRequest = new RegistrationRequest(request.getUsername());
        registrationRequest.setAffiliation(request.getAffliation());
        registrationRequest.setType(request.getType());

        if (request.getGenSecret() && (request.getSecret() == null || request.getSecret().isEmpty())) {
            registrationRequest.setSecret(generateSecret());
        } else {
            registrationRequest.setSecret(request.getSecret());
        }

        registrationRequest.setMaxEnrollments(-1); // unlimited enrollments if needed

        var adminIdentity = wallet.get("admin");
        if (adminIdentity == null)
            throw new Exception("Admin identity not found in the wallet");

        // 1.1 getting the registrar
        var adminUser = new User() {

            final X509Identity x509Identity = (X509Identity) adminIdentity;

            @Override
            public String getName() {
                return "admin";
            }

            @Override
            public Set<String> getRoles() {
                return null;
            }

            @Override
            public String getAccount() {
                return null;
            }

            @Override
            public String getAffiliation() {
                return request.getAffliation();
            }

            @Override
            public Enrollment getEnrollment() {
                return new Enrollment() {
                    @Override
                    public PrivateKey getKey() {
                        return x509Identity.getPrivateKey();
                    }

                    @Override
                    public String getCert() {
                        return Identities.toPemString(x509Identity.getCertificate());
                    }
                };
            }

            @Override
            public String getMspId() {
                return "Org1MSP";
            }
        };

        // register the user
        caClient.register(registrationRequest, adminUser);
        log.info("Successfully registered a new user: {}", request.getUsername());

        // 2. enroll
        var enrollmentRequest = new EnrollmentRequest();
        enrollmentRequest.setProfile("tls");
        enrollmentRequest.addHost("localhost");

        var enrollment = caClient.enroll(request.getUsername(), request.getSecret(), enrollmentRequest);

        // 3. add to wallet
        var certificate = Identities.readX509Certificate(enrollment.getCert());
        var userIdentity = Identities.newX509Identity("Org1MSP", certificate, enrollment.getKey());

        wallet.put("client1", userIdentity);
    }

    private String generateSecret() {
        return UUID.randomUUID().toString();
    }
}
