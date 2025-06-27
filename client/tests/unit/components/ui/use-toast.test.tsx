import { renderHook, act } from "@testing-library/react";
import { useToast } from "@/components/ui/use-toast";

jest.useFakeTimers();

describe("useToast", () => {
  afterEach(() => {
    jest.clearAllTimers();
  });

  it("should initialize with empty toasts array", () => {
    const { result } = renderHook(() => useToast());
    expect(result.current.toasts).toEqual([]);
  });

  it("should add a toast with default variant", () => {
    const { result } = renderHook(() => useToast());

    act(() => {
      result.current.toast({
        title: "Test Toast",
        description: "Test Description",
      });
    });

    expect(result.current.toasts).toHaveLength(1);
    expect(result.current.toasts[0]).toEqual({
      title: "Test Toast",
      description: "Test Description",
      variant: "default",
    });
  });

  it("should add a toast with custom variant", () => {
    const { result } = renderHook(() => useToast());

    act(() => {
      result.current.toast({
        title: "Test Toast",
        description: "Test Description",
        variant: "destructive",
      });
    });

    expect(result.current.toasts).toHaveLength(1);
    expect(result.current.toasts[0]).toEqual({
      title: "Test Toast",
      description: "Test Description",
      variant: "destructive",
    });
  });

  it("should add multiple toasts", () => {
    const { result } = renderHook(() => useToast());

    act(() => {
      result.current.toast({ title: "Toast 1" });
      result.current.toast({ title: "Toast 2" });
    });

    expect(result.current.toasts).toHaveLength(2);
    expect(result.current.toasts[0].title).toBe("Toast 1");
    expect(result.current.toasts[1].title).toBe("Toast 2");
  });

  it("should automatically dismiss toast after 5 seconds", () => {
    const { result } = renderHook(() => useToast());

    act(() => {
      result.current.toast({ title: "Test Toast" });
    });

    expect(result.current.toasts).toHaveLength(1);

    act(() => {
      jest.advanceTimersByTime(5000);
    });

    expect(result.current.toasts).toHaveLength(0);
  });

  it("should dismiss all toasts when dismissToast is called", () => {
    const { result } = renderHook(() => useToast());

    act(() => {
      result.current.toast({ title: "Toast 1" });
      result.current.toast({ title: "Toast 2" });
    });

    expect(result.current.toasts).toHaveLength(2);

    act(() => {
      result.current.dismissToast();
    });

    expect(result.current.toasts).toHaveLength(0);
  });

  it("should handle toast without description", () => {
    const { result } = renderHook(() => useToast());

    act(() => {
      result.current.toast({ title: "Test Toast" });
    });

    expect(result.current.toasts[0]).toEqual({
      title: "Test Toast",
      variant: "default",
    });
  });
});
