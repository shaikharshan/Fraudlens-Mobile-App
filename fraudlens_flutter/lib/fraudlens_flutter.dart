import 'dart:typed_data';

import 'package:flutter/services.dart';

/// Flutter facade for FraudLens on **Android** (native SDK). On **iOS**, most calls are unimplemented unless you add bindings.
class FraudLensFlutter {
  FraudLensFlutter._();

  static const MethodChannel _channel = MethodChannel('fraudlens_flutter');

  /// Same keys as [FraudLensConfig] in Kotlin (use empty string to skip a service).
  static Future<void> initialize(Map<String, dynamic> config) async {
    await _channel.invokeMethod<void>('initialize', config);
  }

  static Future<void> clear() async {
    await _channel.invokeMethod<void>('clear');
  }

  static Future<String?> audioHealth() async {
    return _channel.invokeMethod<String>('audioHealth');
  }

  /// [bytes] is sent to native as a byte array (avoid huge payloads).
  static Future<String?> audioDetect({
    required Uint8List bytes,
    required String filename,
    required String contentType,
    String partName = 'file',
  }) async {
    return _channel.invokeMethod<String>('audioDetect', {
      'bytes': bytes,
      'filename': filename,
      'contentType': contentType,
      'partName': partName,
    });
  }

  static Future<String?> imageHealth() async {
    return _channel.invokeMethod<String>('imageHealth');
  }

  static Future<String?> imageDetect({
    required Uint8List bytes,
    required String filename,
    required String contentType,
    String partName = 'file',
  }) async {
    return _channel.invokeMethod<String>('imageDetect', {
      'bytes': bytes,
      'filename': filename,
      'contentType': contentType,
      'partName': partName,
    });
  }

  static Future<String?> videoHealth() async {
    return _channel.invokeMethod<String>('videoHealth');
  }

  static Future<String?> videoDetect({
    required Uint8List bytes,
    required String filename,
    required String contentType,
    String partName = 'file',
  }) async {
    return _channel.invokeMethod<String>('videoDetect', {
      'bytes': bytes,
      'filename': filename,
      'contentType': contentType,
      'partName': partName,
    });
  }

  static Future<Map<String, dynamic>?> checkIpReputation(String ip) async {
    final Object? r = await _channel.invokeMethod<dynamic>('checkIpReputation', ip);
    return r == null ? null : Map<String, dynamic>.from(r as Map);
  }

  static Future<Map<String, dynamic>?> checkModelHealth() async {
    final Object? r = await _channel.invokeMethod<dynamic>('checkModelHealth');
    return r == null ? null : Map<String, dynamic>.from(r as Map);
  }

  static Future<Map<String, dynamic>?> predictFraud(Map<String, dynamic> input) async {
    final Object? r = await _channel.invokeMethod<dynamic>('predictFraud', input);
    return r == null ? null : Map<String, dynamic>.from(r as Map);
  }

  static Future<Map<String, dynamic>?> analyzeScam(String combinedText) async {
    final Object? r = await _channel.invokeMethod<dynamic>('analyzeScam', combinedText);
    return r == null ? null : Map<String, dynamic>.from(r as Map);
  }
}
