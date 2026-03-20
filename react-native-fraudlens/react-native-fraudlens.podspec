require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "react-native-fraudlens"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.license      = "MIT"
  s.authors      = { "FraudLens" => "dev@example.com" }
  s.homepage     = "https://github.com/your-org/fraudlens"
  s.platforms    = { :ios => "13.0" }
  s.source       = { :path => "." }
  s.source_files = "ios/**/*.{h,m,mm}"
  s.dependency "React-Core"
end
