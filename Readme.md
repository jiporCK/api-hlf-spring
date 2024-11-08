```bash
http POST :8181/api/v1/assets/init
http  GET :8181/api/v1/assets/all

http POST :8181/api/v1/chaincode/basic/InitLedger/invoke 
http :8181/api/v1/chaincode/basic/GetAllAssets/query
```