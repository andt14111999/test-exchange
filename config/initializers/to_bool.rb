String.class_eval do
  def to_bool
    return true if self == true || self =~ (/^(true|t|yes|y|1)$/i)
    return false if self == false || self.blank? || self =~ (/^(false|f|no|n|0)$/i)
    raise ArgumentError.new("invalid value for Boolean: \"#{self}\"")
  end
end

Integer.class_eval do
  def to_bool
    return true if self == 1
    return false if self == 0
    raise ArgumentError.new("invalid value for Boolean: \"#{self}\"")
  end
end

TrueClass.class_eval do
  def to_i; 1; end
  def to_bool; self; end
end

FalseClass.class_eval do
  def to_i; 0; end
  def to_bool; self; end
end

NilClass.class_eval do
  def to_bool; false; end
end
