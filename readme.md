## MULTI-SIGN

### Main class

`MultiSigContract.java`

### Core functions

- `addManager` and `removeManager`
  - The minimum number of signatures is dynamically calculated based on the number of managers
- `transferTo`
  - Create a transfer transaction that requires multiple signatures
- `signTransaction`
  - Execute signature, when the number of signatures reaches the minimum number of signatures, the transfer is performed
    
### Query functions

- `getPendingTransaction`
- `isManager`
- `isCompletedTransactions`
- `getMinSignatures`

### Test steps

- Deployment contract
- call `_payable`, transfer NULS to the contract address
- call `addManager`, add one or more managers (A, B, C)
- call `transferTo`, create a multi-signature transfer
- call `signTransaction`, every manager should call this function until the minimum number of signatures is reached
- done.
