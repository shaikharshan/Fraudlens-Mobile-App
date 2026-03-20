Pod::Spec.new do |s|
  s.name             = 'fraudlens_flutter'
  s.version          = '0.1.0'
  s.summary          = 'FraudLens Flutter plugin (iOS stub; use HTTP from Dart or add native iOS).'
  s.description      = <<-DESC
Wraps the FraudLens Android SDK via method channel. iOS does not bundle the Kotlin SDK.
                       DESC
  s.homepage         = 'https://github.com/your-org/fraudlens'
  s.license          = { :type => 'MIT' }
  s.author           = { 'FraudLens' => 'dev@example.com' }
  s.source           = { :path => '.' }
  s.source_files     = 'Classes/**/*'
  s.dependency 'Flutter'
  s.platform = :ios, '13.0'
  s.pod_target_xcconfig = { 'DEFINES_MODULE' => 'YES' }
  s.swift_version = '5.0'
end
