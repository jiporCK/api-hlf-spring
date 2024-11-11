package anuznomii.lol.apihyperledgerfabricspring.restcontrollers;

import org.hyperledger.fabric.gateway.ContractException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import anuznomii.lol.apihyperledgerfabricspring.services.ChaincodeService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/v1/chaincode")
@RequiredArgsConstructor
public class ChaincodeRestController {
    // inject chaincode service here
    private final ChaincodeService chaincodeService;

    @PostMapping("/{chaincodeName}/{functionName}/invoke")
    public String invokeChaincode(
            @PathVariable String chaincodeName,
            @PathVariable String functionName

    ) throws Exception {
        chaincodeService.invokeChaincode(chaincodeName,
                functionName, "null");
        return "Invoke chaincode " + functionName + " successfully!!!";
    }

    @GetMapping("/{chaincodeName}/{functionName}/query")
    public ResponseEntity<String> queryChaincode(
            @PathVariable String chaincodeName,
            @PathVariable String functionName,
            @RequestParam(required = false) String assetId) {

        try {
            String result = chaincodeService.queryChaincode(chaincodeName, functionName, assetId);
            return ResponseEntity.ok(result);
        } catch (ContractException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Asset not found or query error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error executing query: " + e.getMessage());
        }
    }


}