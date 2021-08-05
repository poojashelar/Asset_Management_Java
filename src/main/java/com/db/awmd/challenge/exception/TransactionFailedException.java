package com.db.awmd.challenge.exception;

public class TransactionFailedException extends RuntimeException{
    public TransactionFailedException(String message){ super(message); }
}
