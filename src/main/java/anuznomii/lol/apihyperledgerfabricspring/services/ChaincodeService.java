package anuznomii.lol.apihyperledgerfabricspring.services;

import java.util.concurrent.TimeoutException;

import org.hyperledger.fabric.gateway.ContractException;
import org.hyperledger.fabric.gateway.Gateway;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChaincodeService {

    private final Gateway gateway;

    // 1. create invoke method
    public void invokeChaincode(String chaincodeName,
                                String methodName, String... args) throws ContractException, TimeoutException, InterruptedException {

        // try catch this
        var network = gateway.getNetwork("mychannel");
        var contract = network.getContract(chaincodeName); // basic-> is our chaicnode name

        var result = contract.submitTransaction(methodName, args);
        log.info("result : {}", result);

    }

    // 2. create query method
    public String queryChaincode(String chaincodeName, String methodName, String... args) throws ContractException {
        var network = gateway.getNetwork("mychannel");
        var contract = network.getContract(chaincodeName);

        byte[] result;
        if (args.length > 0 && args[0] != null && !args[0].isEmpty()) {
            result = contract.evaluateTransaction(methodName, args);
        } else {
            // Call a chaincode function without args or a default function
            result = contract.evaluateTransaction(methodName);
        }
        return new String(result);
    }



}