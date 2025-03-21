# SnowFox Exchange

A robust cryptocurrency exchange platform built with Ruby on Rails, featuring Kafka-based event processing for real-time transaction handling across multiple blockchain networks.

## Features

### Cryptocurrency Support

- Multi-currency support (ETH, USDT, BNB, etc.)
- Multi-network integration (ERC20, BEP20, TRC20)
- Automated wallet address management
- Smart address reuse for tokens on the same network

### Account Management

- User authentication with social login
- Role-based access control (user/merchant)
- Automated account creation
- Separate deposit and main accounts
- Real-time balance tracking

### Transaction Processing

- Cryptocurrency deposits with confirmation tracking
- Secure withdrawal management
- Real-time balance updates
- Transaction verification system
- Fee handling system

## Technical Stack

- Ruby on Rails
- PostgreSQL
- Apache Kafka
- Redis
- Ed25519 for cryptographic signatures

## Prerequisites

- Ruby 3.x
- PostgreSQL
- Apache Kafka
- Redis

## Environment Setup

Required environment variables:

```bash
KAFKA_BROKER=your_kafka_broker_address
COIN_PORTAL_VERIFYING_KEY=your_verification_key
```

## Installation

1. Clone the repository

```bash
git clone <repository_url>
cd snowfox-exchange
```

2. Install dependencies

```bash
bundle install
```

3. Setup database

```bash
rails db:create
rails db:migrate
```

4. Start services

```bash
# Start Kafka consumer
rails kafka:consumer:start

# Start Rails server
rails server
```

## Kafka Topics

### Input Topics

- `EE.I.coin_account`: Account management
- `EE.I.coin_deposit`: Deposit processing
- `EE.I.coin_withdraw`: Withdrawal handling
- `EE.I.coin_account_query`: Balance queries
- `EE.I.coin_account_reset`: Account resets

### Output Topics

- `EE.O.coin_account_update`: Balance updates
- `EE.O.transaction_response`: Transaction results

## API Authentication

All API requests require the following headers:

```ruby
{
  'X-Signature' => 'Ed25519 signature',
  'X-Timestamp' => 'Current timestamp',
  'Content-Type' => 'application/json'
}
```

## Testing

Run the test suite:

```bash
bundle exec rspec
```

Run specific tests:

```bash
bundle exec rspec spec/controllers/coin_portal_controller_spec.rb
```

## Architecture Overview

### Services

- `KafkaService::ConsumerManager`: Manages Kafka consumers
- `KafkaService::Services::Coin::CoinAccountService`: Account operations
- `KafkaService::Services::Coin::CoinDepositService`: Deposit handling
- `KafkaService::Services::Coin::CoinWithdrawalService`: Withdrawal processing

### Event Handlers

- `CoinAccountHandler`: Processes account events
- `CoinDepositHandler`: Handles deposit events
- `BaseHandler`: Base handler functionality

### Models

- `User`: User management
- `CoinAccount`: Cryptocurrency accounts
- `CoinDeposit`: Deposit operations
- `CoinWithdrawal`: Withdrawal operations
- `SocialAccount`: Social authentication

## Security Features

- Ed25519 signature verification
- Request timestamp validation
- Role-based access control
- Secure wallet management
- Transaction verification

## Error Handling

- Automatic retry mechanism
- Transaction rollback protection
- Dead letter queue for failed messages
- Comprehensive error logging

## Contributing

1. Fork the repository
2. Create your feature branch
3. Write tests for new features
4. Submit a pull request

## License

This project is licensed under the MIT License.

## Support

For support and documentation, please refer to the `/docs` directory or contact the development team.
