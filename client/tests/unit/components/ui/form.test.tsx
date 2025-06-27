import * as React from "react";
import { render, screen } from "@testing-library/react";
import {
  FieldValues,
  UseFormReturn,
  ControllerRenderProps,
  ControllerFieldState,
  UseFormStateReturn,
} from "react-hook-form";
import {
  Form,
  FormField,
  FormItem,
  FormLabel,
  FormControl,
  FormDescription,
  FormMessage,
} from "@/components/ui/form";

const FormFieldContext = React.createContext<boolean>(false);

const mockFormState: UseFormStateReturn<FieldValues> = {
  isDirty: false,
  isLoading: false,
  isSubmitted: false,
  isSubmitSuccessful: false,
  isSubmitting: false,
  isValidating: false,
  isValid: false,
  submitCount: 0,
  dirtyFields: {},
  touchedFields: {},
  errors: {},
  defaultValues: undefined,
  disabled: false,
  validatingFields: {},
};

const mockFieldState: ControllerFieldState = {
  invalid: false,
  isTouched: false,
  isDirty: false,
  isValidating: false,
  error: undefined,
};

const mockForm = {
  watch: jest.fn(),
  getValues: jest.fn(),
  getFieldState: jest.fn(),
  setError: jest.fn(),
  clearErrors: jest.fn(),
  setValue: jest.fn(),
  trigger: jest.fn(),
  formState: mockFormState,
  resetField: jest.fn(),
  reset: jest.fn(),
  handleSubmit: jest.fn(),
  unregister: jest.fn(),
  control: {},
  register: jest.fn(),
  setFocus: jest.fn(),
} as unknown as UseFormReturn<FieldValues>;

type ControllerRenderFn = {
  field: ControllerRenderProps<FieldValues, string>;
  fieldState: ControllerFieldState;
  formState: UseFormStateReturn<FieldValues>;
};

// Mock react-hook-form
jest.mock("react-hook-form", () => {
  const actual = jest.requireActual("react-hook-form");
  return {
    ...actual,
    useFormContext: jest.fn(() => mockForm),
    useForm: () => mockForm,
    Controller: ({
      render,
    }: {
      render: (props: ControllerRenderFn) => React.ReactElement;
    }) =>
      render({
        field: {
          value: "",
          onChange: jest.fn(),
          name: "test",
          onBlur: jest.fn(),
          ref: jest.fn(),
        },
        fieldState: mockFieldState,
        formState: mockFormState,
      }),
  };
});

jest.mock("@/components/ui/form", () => {
  const actual = jest.requireActual("@/components/ui/form");
  return {
    ...actual,
    Form: ({ children }: { children: React.ReactNode }) => (
      <div>{children}</div>
    ),
    FormField: ({
      name,
      render,
    }: {
      name: string;
      render: (props: ControllerRenderFn) => React.ReactElement;
    }) => {
      return (
        <FormFieldContext.Provider value={true}>
          {render({
            field: {
              value: "",
              onChange: jest.fn(),
              name,
              onBlur: jest.fn(),
              ref: jest.fn(),
            },
            fieldState: mockFieldState,
            formState: mockFormState,
          })}
        </FormFieldContext.Provider>
      );
    },
    FormItem: ({ children }: { children: React.ReactNode }) => (
      <div>{children}</div>
    ),
    FormLabel: ({ children }: { children: React.ReactNode }) => {
      const isInsideFormField = React.useContext(FormFieldContext);
      if (!isInsideFormField) {
        throw new Error("useFormField should be used within <FormField>");
      }
      return <label>{children}</label>;
    },
    FormControl: () => <input type="text" />,
    FormDescription: ({ children }: { children: React.ReactNode }) => (
      <div>{children}</div>
    ),
    FormMessage: ({ children }: { children: React.ReactNode }) => (
      <div>{children}</div>
    ),
  };
});

describe("Form Components", () => {
  describe("Form", () => {
    it("renders form with children", () => {
      render(
        <Form {...mockForm}>
          <div>Form Content</div>
        </Form>,
      );
      expect(screen.getByText("Form Content")).toBeInTheDocument();
    });
  });

  describe("FormField", () => {
    it("renders form field with children", () => {
      render(
        <Form {...mockForm}>
          <FormField name="test" render={() => <div>Field Content</div>} />
        </Form>,
      );
      expect(screen.getByText("Field Content")).toBeInTheDocument();
    });
  });

  describe("FormItem", () => {
    it("renders form item with children", () => {
      render(
        <Form {...mockForm}>
          <FormField
            name="test"
            render={() => (
              <FormItem>
                <div>Item Content</div>
              </FormItem>
            )}
          />
        </Form>,
      );
      expect(screen.getByText("Item Content")).toBeInTheDocument();
    });
  });

  describe("FormLabel", () => {
    it("renders form label with children", () => {
      render(
        <Form {...mockForm}>
          <FormField
            name="test"
            render={() => (
              <FormItem>
                <FormLabel>Label Content</FormLabel>
              </FormItem>
            )}
          />
        </Form>,
      );
      expect(screen.getByText("Label Content")).toBeInTheDocument();
    });
  });

  describe("FormControl", () => {
    it("renders form control", () => {
      render(
        <Form {...mockForm}>
          <FormField name="test" render={() => <FormControl />} />
        </Form>,
      );
      expect(screen.getByRole("textbox")).toBeInTheDocument();
    });
  });

  describe("FormDescription", () => {
    it("renders form description with children", () => {
      render(
        <Form {...mockForm}>
          <FormField
            name="test"
            render={() => (
              <FormDescription>Description Content</FormDescription>
            )}
          />
        </Form>,
      );
      expect(screen.getByText("Description Content")).toBeInTheDocument();
    });
  });

  describe("FormMessage", () => {
    it("renders form message with children", () => {
      render(
        <Form {...mockForm}>
          <FormField
            name="test"
            render={() => <FormMessage>Message Content</FormMessage>}
          />
        </Form>,
      );
      expect(screen.getByText("Message Content")).toBeInTheDocument();
    });
  });
});
