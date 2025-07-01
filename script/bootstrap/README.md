# Bootstrap Scripts

This directory contains scripts to set up complete merchant trading environments for different fiat currencies for testing and development purposes.

## Structure

The bootstrap scripts are organized by currency:

- **`vnd/`** - Vietnamese Dong (VND) market setup
- **`ngn/`** - Nigerian Naira (NGN) market setup

## Overview

Each currency bootstrap process creates a merchant user and sets up a complete trading environment including:

- USDT deposits
- Merchant escrow conversions
- AMM pool and liquidity positions
- Fiat currency buy/sell offers

## Scripts (per currency)

- **create_merchant_user.rb** - Creates the merchant user with proper role and status
- **first_usdt_deposit.rb** - Creates first 200k USDT deposit
- **first_merchant_escrow.rb** - Converts 100k USDT to fiat via merchant escrow
- **setup_amm_pool.rb** - Creates and activates USDT/fiat AMM pool
- **create_amm_position.rb** - Adds 100k USDT liquidity to the AMM pool
- **second_usdt_deposit.rb** - Creates second 200k USDT deposit
- **second_merchant_escrow.rb** - Converts 200k USDT to fiat via merchant escrow
- **create_offers.rb** - Creates fiat buy and sell offers for P2P trading

## Usage

### Run All Scripts for a Currency

```bash
# VND market setup
rails runner script/bootstrap/vnd/run_all.rb

# NGN market setup
rails runner script/bootstrap/ngn/run_all.rb
```

### Run Individual Scripts

```bash
# VND scripts
rails runner script/bootstrap/vnd/create_merchant_user.rb
rails runner script/bootstrap/vnd/setup_amm_pool.rb

# NGN scripts
rails runner script/bootstrap/ngn/create_merchant_user.rb
rails runner script/bootstrap/ngn/setup_amm_pool.rb
```

## What Gets Created

### VND Market (Vietnam)
- **User**: mikevn@example.com (merchant role, active status)
- **USDT Deposits**: 2 deposits of 200k USDT each (400k total)
- **Merchant Escrows**: 2 conversions totaling 300k USDT → 8.1B VND
- **AMM Pool**: USDT/VND pool with 0.3% fee
- **AMM Position**: 100k USDT liquidity in price range 26,000-26,500 VND
- **Offers**: Buy and sell VND offers with 100k VND fixed fee

### NGN Market (Nigeria)
- **User**: mikeng@example.com (merchant role, active status)
- **USDT Deposits**: 2 deposits of 200k USDT each (400k total)
- **Merchant Escrows**: 2 conversions totaling 300k USDT → 300M NGN
- **AMM Pool**: USDT/NGN pool with 0.3% fee
- **AMM Position**: 100k USDT liquidity in price range 950-1,050 NGN
- **Offers**: Buy and sell NGN offers with 5k NGN fixed fee

## Exchange Rates Used

### VND (Vietnamese Dong)
- USDT/VND: 27,000 VND per USDT
- AMM price range: 26,000 - 26,500 VND per USDT
- AMM initial price: 26,100 VND per USDT

### NGN (Nigerian Naira)
- USDT/NGN: 1,000 NGN per USDT
- AMM price range: 950 - 1,050 NGN per USDT
- AMM initial price: 1,000 NGN per USDT

## Technical Details

### Tick Value Validation

The AMM setup scripts include mathematical validation to ensure tick values are calculated correctly using the Uniswap V3 formula:

```ruby
tick = (Math.log(price) / Math.log(1.0001)).round
```

If admin-provided tick values don't match the formula results, the scripts will panic and exit with detailed error messages.

### Kafka Integration

The application uses **rdkafka** (librdkafka Ruby bindings) for reliable Kafka integration with better native library management on macOS. This resolves previous `dyld: missing symbol called` errors that occurred with the `ruby-kafka` gem.

## Troubleshooting

### ✅ dyld: missing symbol called Error (RESOLVED)

~~If you encounter `dyld[xxxxx]: missing symbol called` errors when running the scripts, this is typically caused by the `ruby-kafka` gem having compatibility issues on macOS (especially Apple Silicon).~~

**✅ RESOLVED**: The application has been successfully migrated from `ruby-kafka` to `rdkafka` which provides better native library management and resolves the dyld errors on macOS.

### Tick Value Validation Errors

If you see tick validation errors like:
```
❌ PANIC: Lower tick mismatch!
   Expected (formula): 101664
   Got (admin): 101660
```

This means the hardcoded tick values don't match the mathematical formula. Update the admin-provided values to match the formula results.

## Notes

- All USDT deposits use fake transaction hashes for testing
- The merchant users can access escrow functionality due to merchant role
- AMM positions provide liquidity in tight price ranges for efficient trading
- Offers are set to automatic and online for immediate availability
- Each currency market is completely independent with separate users and pools
- Kafka integration uses rdkafka for reliable cross-platform compatibility 