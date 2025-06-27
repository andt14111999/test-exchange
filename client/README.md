# Exchange Client

A modern cryptocurrency exchange client built with Next.js 15, supporting both AMM (Automated Market Maker) and P2P trading functionalities.

## Features

### Trading

- **AMM Trading**

  - Real-time price calculation using sqrt price algorithm
  - Support for multiple trading pairs (USDT/VND, USDT/PHP, USDT/NGN)
  - Liquidity pool management
  - Real-time exchange rate updates

- **P2P Trading**
  - Direct peer-to-peer trading
  - Multiple payment methods support
  - QR code payment integration
  - Payment proof upload system
  - Order management system

### Wallet

- Multi-currency wallet support
- Deposit and withdrawal functionality
- Transaction history tracking
- Real-time balance updates

### User Experience

- Responsive design
- Dark/Light mode support
- Multi-language support (English, Vietnamese)
- Real-time notifications
- QR code scanning for payments

## Tech Stack

### Core

- Next.js 15 (App Router)
- React 18
- TypeScript
- Tailwind CSS

### State Management & Data

- Zustand for global state
- TanStack Query for API management
- BigNumber.js for precise calculations

### UI Components

- shadcn/ui
- Lucide React icons
- Sonner for toast notifications

### Forms & Validation

- React Hook Form
- Zod for schema validation

## Project Structure

```text
src/
├── app/                    # Next.js app router pages
│   └── [locale]/          # Localized routes
├── components/            # Reusable React components
│   ├── amm/              # AMM trading components
│   ├── deposit/          # Deposit related components
│   ├── p2p/              # P2P trading components
│   └── ui/               # UI components
├── hooks/                # Custom React hooks
├── i18n/                 # Translation files
├── lib/                  # Utility functions and configurations
│   ├── api/             # API client and hooks
│   ├── store/           # Zustand stores
│   └── utils/           # Helper functions
└── types/               # TypeScript type definitions
```

## Getting Started

1. Install dependencies:

```bash
pnpm install
```

1. Set up environment variables:

```bash
cp .env.example .env.local
```

1. Run the development server:

```bash
pnpm dev
```

1. Open [http://localhost:3000](http://localhost:3000)

## Environment Variables

Required environment variables:

```env
NEXT_PUBLIC_API_URL=your_api_url
```

## Development

### Prerequisites

- Node.js 18 or later
- pnpm 8 or later
- Git

### Development Dependencies

The project uses several development tools:

- **TypeScript** - Type checking
- **ESLint** - Code linting
- **Prettier** - Code formatting
- **ts-node** - TypeScript execution

### Available Scripts

- `pnpm dev` - Start development server
- `pnpm build` - Build for production
- `pnpm start` - Start production server
- `pnpm lint` - Run ESLint
- `pnpm format` - Format code with Prettier
- `pnpm type-check` - Run TypeScript type checking

### Code Style

The project uses:

- ESLint for code linting
- Prettier for code formatting
- TypeScript for type checking

To format code:

```bash
pnpm format
```

To check types:

```bash
pnpm type-check
```

### Translation Management

The project uses next-intl for internationalization. Translation files are located in `src/i18n/`.

To check for missing or redundant translations:

```bash
pnpm check-translations
```

## License

MIT
