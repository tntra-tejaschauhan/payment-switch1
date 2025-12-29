# Mini payment switch - ISO 8583 Implementation

### A payment switch built with java 21+,JPOS, spring boot and spring integration that processes ISO 8583 messages from ATMs and route them to banking servers.

## Feature

- ISO 8583 Message Processing: Full support for parsing and generating ISO 8583 messages
- Java 21 Records: Immutable domain models with built-in validation
- Sealed Interfaces: Type-safe processing codes and validation results
- Pattern Matching: Modern switch expressions with record patterns
- Guarded Patterns: Advanced routing logic with when clauses
- Transaction Routing: Smart routing based on Processing Code
- Validation: Comprehensive message validation for mandatory fields
- Bank Simulation: Built-in bank simulator for testing various transaction types
- TCP Communication: Spring Integration TCP support for ATM connections
- Logging: Comprehensive logging with Log4j2
- Error Handling: Robust error handling with appropriate response codes

## Components

- TcpServerConfig: Configures TCP server on port 8583
- ByteArrayLengthHeader@ByteSerializer: serialization configuration for ISO message header (which contain the length of message) 2 bytes header length (default is 4 bytes) 
- IsoMessageHandler: Main message processing handler
- IsoMessageValidator: Validates mandatory ISO 8583 fields
- IsoMessageTransformer: Converts ISO messages ↔ Domain models ( Transaction request and response models)
- TransactionRouter: Routes transactions based on Processing Code
- BankSimulatorService: Simulates bank responses


```aiignore
payment-switch/
├── src/main/java/com/paymentswitch/
│   ├── PaymentSwitchApplication.java
│   ├── config/
│   │   ├── JposConfig.java
│   │   └── TcpServerConfig.java
│   │   └── ByteArrayLengthHeader@ByteSerializer.java
│   ├── handler/
│   │   └── IsoMessageHandler.java (Java 21 pattern matching)
│   ├── model/
│   │   ├── TransactionRequest.java (Record)
│   │   ├── TransactionResponse.java (Record)
│   │   ├── ProcessingCode.java (Sealed interface)
│   │   ├── ValidationResult.java (Sealed interface)
│   │   └── ResponseCode.java (Enum)
│   ├── validator/
│   │   └── IsoMessageValidator.java (Pattern matching)
│   ├── transformer/
│   │   └── IsoMessageTransformer.java
│   ├── router/
│   │   └── TransactionRouter.java (Record pattern matching)
│   ├── service/
│   │   └── BankSimulatorService.java (Sealed results)
│   ├── client/
│   │   └── AtmSimulatorClient.java
│   └── examples/
│       └── Java21FeaturesShowcase.java
├── src/main/resources/
│   ├── application.yml
│   └── packager/
│       └── iso87ascii.xml
└── pom.xml
```

## Message Type Indicator (MTI)

- The MTI is a 4-digit code that identifies the message type:
```aiignore
Position 1: Version
  0 = ISO 8583:1987
  1 = ISO 8583:1993
  2 = ISO 8583:2003

Position 2: Message Class
  0 = Reserved
  1 = Authorization
  2 = Financial
  3 = File Action
  4 = Reversal/Chargeback

Position 3: Message Function
  0 = Request
  1 = Response
  2 = Advice
  3 = Advice Response

Position 4: Transaction Originator
  0 = Acquirer
  1 = Issuer
  2 = Other
```
## Common MTIs:

```aiignore
0200: Financial Transaction Request (e.g., withdrawal, purchase)
0210: Financial Transaction Response
0420: Reversal Request
0421: Reversal Response
0800: Network Management Request
0810: Network Management Response
```

## Key Fields Explained

```aiignore
    Field         Name          Format         Example         Description
    
    0           MTI                     n4               0200                Message type Indicator
    2           PAN                     n..19           4111111111111111        Primary account number (card number)    
    3           Processing code         n6              310000                  Transaction Type 
    4           Amount                  n12             000000010000            Transcation amount
    7           DateTime                n10             1232344543              Transmission date/time(MMddHHmmss)
    11          STAN                    n6              123456                  System tace audit number(unique ID)
    32          Acquiring Institution   n..11       123456                  Bank/Institution code
    37          Retival Refrence        an12        123456789012            unique transaction reference
    38          Authorization code      an6         ABC123                  Approval code from bank       
    39          Response code           an12        00                      Result code
    41          Terminal ID             ans8        ATM00001                ATM/POS Terminal Identifier 
    43          Card Accepter Name      ans40       ATM MAIN ST             Terminal Location   
    48          Additional Data         ans..999     AVAIL:12000.0          Extra Transaction data
```

## Supported Transaction Types
```
      Processing Code      Transaction Type        Description
            
        31xxxx          Balance Inquiry         Check account balance
        01xxxx          Withdrawal              Cash withdrawal
        00xxxx          Purchase                Point of sale purchase
        40xxxx          Transfer                Fund transfer (account to account)
        38xxxx          Mini Statement          Request mini statement
```

## Installation

- 1. Clone the repo - https://github.com/tntra-tejaschauhan/payment-switch1.git
- 2. Reload maven project - pom.xml
- 3. Run Server paymentSwitchApplication.java - server running at 8081
- 4. run client (for testing) AtmSimulatorClient.java - client running at 8080
- 5. switch port is 8583


## ISO 8583 Message Flow

### Request (MTI 0200 - Financial Transaction Request)

```aiignore
Field 0  : 0200 (MTI)
Field 2  : 4111111111111111 (PAN)
Field 3  : 310000 (Processing Code - Balance Inquiry)
Field 4  : 000000010000 (Amount - $100.00)
Field 7  : 1213120000 (Transmission DateTime)
Field 11 : 123456 (STAN)
Field 32 : 123456 (Acquiring Institution)
Field 41 : ATM00001 (Terminal ID)
Field 43 : TEST ATM LOCATION (Terminal Name/Location)
```

### Response (MTI 0210 - Financial Transaction Response)

```aiignore
Field 0  : 0210 (MTI)
Field 2  : 4111111111111111 (PAN)
Field 3  : 310000 (Processing Code)
Field 11 : 123456 (STAN)
Field 38 : 123456 (Authorization Code)
Field 39 : 00 (Response Code - Approved)
Field 48 : AVAIL:25000.00|LEDGER:25500.00 (Additional Data)
```

## Response Code

```aiignore
    Code          Description
    
    00          Approved
    12          Invalid Transation
    13          Invalid Amount
    14          Invalid card number
    30          Format Error
    51          Insufficient funds
    54          Expired card
    55          Incorrect PIN
    57          Transaction not permitted
    61          Exceeds withdrawal limits
    96          System malfunction
```

## ISO 8583 Message Structure

```aiignore
┌─────────────────────────────────────────────────────────┐
│                    ISO 8583 MESSAGE                      │
├──────────┬──────────┬───────────────────────────────────┤
│   MTI    │  BITMAP  │          DATA FIELDS              │
│ (4 bytes)│(8/16 byt)│        (Variable Length)          │
└──────────┴──────────┴───────────────────────────────────┘

Example:
0200│3220000000000000│4111111111111111│310000│...
 ↑       ↑                  ↑              ↑
MTI   Bitmap          Field 2 (PAN)   Field 3 (Proc Code)
```

## Message Format:

```aiignore
┌──────────┬────────────────────────────┐
│  Length  │      Message Payload       │
│ (2 bytes)│      (Variable bytes)      │
└──────────┴────────────────────────────┘

Example:
0x00 0x80 │ 0200... (128 bytes of ISO message)
  ↑   ↑
  |   └─ Low byte
  └───── High byte
```

## Logs are written to:

- Console: Real-time output
- File: logs/payment-switch.log

## Dependencies

- Spring Boot 3.2.0
- Spring Integration (TCP)
- jPOS 2.1.8
- Lombok 1.18.30
- Log4j2

## Learning Resources

- [ISO 8583 Standard](https://en.wikipedia.org/wiki/ISO_8583)
- [jPOS Documentation](https://jpos.org/docs/category/intro)
- [Spring Integration](https://spring.io/projects/spring-integration)
- [Java 21 Pattern Matching](https://openjdk.org/jeps/441)
- [Java 21 Record Patterns](https://openjdk.org/jeps/440)