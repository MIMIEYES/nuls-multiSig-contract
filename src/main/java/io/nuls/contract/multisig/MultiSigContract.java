package io.nuls.contract.multisig;

import io.nuls.contract.multisig.event.DepositFunds;
import io.nuls.contract.multisig.event.TransferFunds;
import io.nuls.contract.multisig.event.TransferTransactionCreated;
import io.nuls.contract.multisig.model.TransferTransaction;
import io.nuls.contract.ownership.Ownable;
import io.nuls.contract.sdk.Address;
import io.nuls.contract.sdk.Contract;
import io.nuls.contract.sdk.Msg;
import io.nuls.contract.sdk.annotation.JSONSerializable;
import io.nuls.contract.sdk.annotation.Payable;
import io.nuls.contract.sdk.annotation.View;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static io.nuls.contract.sdk.Utils.emit;
import static io.nuls.contract.sdk.Utils.require;

/**
 * @author: PierreLuo
 * @date: 2021/8/6
 */
public class MultiSigContract extends Ownable implements Contract {

    private Set<String> managers = new HashSet<String>();
    private Set<Integer> completedTransactions = new HashSet<Integer>();
    private Map<Integer, TransferTransaction> pendingTransfer = new HashMap<Integer, TransferTransaction>();
    // Minimum signature ratio 66%
    private int RATIO = 66;
    // denominator
    private int DENOMINATOR = 100;
    private int transactionIdx = 0;
    private int MIN_SIGNATURES = 1;


    private void onlyManager() {
        require(managers.contains(Msg.sender().toString()), "Only the managers of the contract can execute it.");
    }

    public MultiSigContract(){
        managers.add(Msg.sender().toString());
    }

    @Payable
    @Override
    public void _payable() {
        emit(new DepositFunds(Msg.sender().toString(), Msg.value()));
    }

    public void addManager(Address manager) {
        onlyOwner();
        managers.add(manager.toString());
        MIN_SIGNATURES = this.calMinSignatures(managers.size());
    }

    public void removeManager(Address manager) {
        onlyOwner();
        managers.remove(manager.toString());
        MIN_SIGNATURES = this.calMinSignatures(managers.size());
    }

    public void transferTo(Address to, BigInteger amount) {
        onlyManager();
        require(Msg.address().balance().compareTo(amount) >= 0, "This contract address does not have sufficient balance");
        int transactionId = transactionIdx++;
        String sender = Msg.sender().toString();
        String _to = to.toString();

        TransferTransaction transferTransaction = new TransferTransaction();
        transferTransaction.setFrom(sender);
        transferTransaction.setTo(_to);
        transferTransaction.setAmount(amount);
        transferTransaction.increaseSignatureCount();
        transferTransaction.getSignatures().add(sender);
        pendingTransfer.put(transactionId, transferTransaction);
        emit(new TransferTransactionCreated(sender, _to, amount, transactionId));

        if (transferTransaction.getSignatureCount() >= MIN_SIGNATURES) {
            this.executeTransfer(transactionId, transferTransaction);
        }
    }

    public void signTransaction(Integer transactionId) {
        onlyManager();
        require(!completedTransactions.contains(transactionId), "Transaction has been completed");
        TransferTransaction transferTransaction = pendingTransfer.get(transactionId);
        require(transferTransaction != null, "Transaction Not Exist");
        String sender = Msg.sender().toString();
        require(!transferTransaction.getSignatures().contains(sender), "Duplicate signature");
        transferTransaction.getSignatures().add(sender);
        transferTransaction.increaseSignatureCount();

        if (transferTransaction.getSignatureCount() >= MIN_SIGNATURES) {
            this.executeTransfer(transactionId, transferTransaction);
        }
    }

    private void executeTransfer(Integer transactionId, TransferTransaction transferTransaction) {
        BigInteger amount = transferTransaction.getAmount();
        String to = transferTransaction.getTo();
        require(Msg.address().balance().compareTo(amount) >= 0, "This contract address does not have sufficient balance");
        new Address(to).transfer(amount);
        completedTransactions.add(transactionId);
        pendingTransfer.remove(transactionId);
        emit(new TransferFunds(to, amount, transactionId));
    }

    @View
    @JSONSerializable
    public TransferTransaction getPendingTransaction(Integer transactionId) {
        return pendingTransfer.get(transactionId);
    }

    @View
    public boolean isManager(Address address) {
        return managers.contains(address.toString());
    }

    @View
    public boolean isCompletedTransactions(Integer transactionId) {
        return completedTransactions.contains(transactionId);
    }

    @View
    public int getMinSignatures() {
        return MIN_SIGNATURES;
    }

    private int calMinSignatures(int managerCounts) {
        if (managerCounts == 0) {
            return 0;
        }
        int numerator = RATIO * managerCounts + DENOMINATOR - 1;
        return numerator / DENOMINATOR;
    }
}