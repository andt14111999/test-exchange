import { QueryClient } from "@tanstack/react-query";
import { queryClient } from "@/lib/query/query-client";

describe("queryClient", () => {
  beforeEach(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
    queryClient.clear();
  });

  it("should be an instance of QueryClient", () => {
    expect(queryClient).toBeInstanceOf(QueryClient);
  });

  it("should have correct default options", () => {
    const defaultOptions = queryClient.getDefaultOptions();

    expect(defaultOptions.queries).toBeDefined();
    expect(defaultOptions.queries?.refetchOnWindowFocus).toBe(false);
    expect(defaultOptions.queries?.retry).toBe(1);
    expect(defaultOptions.queries?.staleTime).toBe(5 * 60 * 1000); // 5 minutes
  });

  it("should handle query invalidation", async () => {
    // Setup a mock query
    const queryKey = ["test"];
    const queryFn = jest.fn().mockResolvedValue("test data");

    // Fetch the query initially
    await queryClient.fetchQuery({ queryKey, queryFn });

    // Verify data is in cache
    expect(queryClient.getQueryData(queryKey)).toBe("test data");

    // Invalidate the query
    await queryClient.invalidateQueries({ queryKey });

    // Verify query is marked as stale
    const query = queryClient.getQueryState(queryKey);
    expect(query?.isInvalidated).toBe(true);
  });

  it("should handle query caching", async () => {
    const queryKey = ["testCache"];
    const queryFn = jest.fn().mockResolvedValue("cached data");

    // First query execution
    await queryClient.fetchQuery({
      queryKey,
      queryFn,
    });

    // Second query execution should use cached data
    await queryClient.fetchQuery({
      queryKey,
      queryFn,
    });

    // QueryFn should only be called once due to caching
    expect(queryFn).toHaveBeenCalledTimes(1);
  });
});
