import { NextRequest } from "next/server";

// Mock the dependencies
jest.mock("@/config/i18n", () => ({
  defaultLocale: "en",
  locales: ["en", "vi", "fil"],
}));

// Mock the NextResponse and NextRequest
jest.mock("next/server", () => ({
  NextResponse: {
    next: jest.fn(() => ({ status: 200 })),
  },
  NextRequest: jest.fn().mockImplementation(() => ({
    nextUrl: { pathname: "/test" },
  })),
}));

// Mock the intl middleware function
const mockIntlMiddlewareHandler = jest.fn().mockReturnValue({ status: 200 });
const mockCreateIntlMiddleware = jest
  .fn()
  .mockReturnValue(mockIntlMiddlewareHandler);

jest.mock("next-intl/middleware", () => {
  return mockCreateIntlMiddleware;
});

// Import the middleware after mocks are setup
const { default: middleware, config } = jest.requireActual("@/middleware");

describe("Middleware", () => {
  it("should call intlMiddleware and return response", () => {
    // Create a simple mock request
    const req = { nextUrl: { pathname: "/test" } } as unknown as NextRequest;

    // Call middleware with mock request
    const response = middleware(req);

    // Verify response is returned
    expect(response).toEqual({ status: 200 });

    // Verify middleware handler was called with the request
    expect(mockIntlMiddlewareHandler).toHaveBeenCalledWith(req);
  });

  it("should export the correct matcher config", () => {
    expect(config).toEqual({
      matcher: ["/((?!api|_next|_vercel|.*\\..*).*)"],
    });
  });
});
