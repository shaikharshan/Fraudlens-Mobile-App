#import "FraudLens.h"

@implementation FraudLens

RCT_EXPORT_MODULE(FraudLens);

static void rejectAll(RCTPromiseRejectBlock reject) {
  reject(@"E_UNIMPLEMENTED", @"FraudLens native SDK is Android-only in this package. Use fetch/axios to your APIs on iOS or extend this module.", nil);
}

RCT_EXPORT_METHOD(initialize : (NSDictionary *)config resolver : (RCTPromiseResolveBlock)resolve rejecter : (RCTPromiseRejectBlock)reject) {
  rejectAll(reject);
}

RCT_EXPORT_METHOD(clear : (RCTPromiseResolveBlock)resolve rejecter : (RCTPromiseRejectBlock)reject) {
  rejectAll(reject);
}

RCT_EXPORT_METHOD(audioHealth : (RCTPromiseResolveBlock)resolve rejecter : (RCTPromiseRejectBlock)reject) {
  rejectAll(reject);
}

RCT_EXPORT_METHOD(audioDetectBase64 : (NSDictionary *)payload resolver : (RCTPromiseResolveBlock)resolve rejecter : (RCTPromiseRejectBlock)reject) {
  rejectAll(reject);
}

RCT_EXPORT_METHOD(imageHealth : (RCTPromiseResolveBlock)resolve rejecter : (RCTPromiseRejectBlock)reject) {
  rejectAll(reject);
}

RCT_EXPORT_METHOD(imageDetectBase64 : (NSDictionary *)payload resolver : (RCTPromiseResolveBlock)resolve rejecter : (RCTPromiseRejectBlock)reject) {
  rejectAll(reject);
}

RCT_EXPORT_METHOD(videoHealth : (RCTPromiseResolveBlock)resolve rejecter : (RCTPromiseRejectBlock)reject) {
  rejectAll(reject);
}

RCT_EXPORT_METHOD(videoDetectBase64 : (NSDictionary *)payload resolver : (RCTPromiseResolveBlock)resolve rejecter : (RCTPromiseRejectBlock)reject) {
  rejectAll(reject);
}

RCT_EXPORT_METHOD(checkIpReputation : (NSString *)ip resolver : (RCTPromiseResolveBlock)resolve rejecter : (RCTPromiseRejectBlock)reject) {
  rejectAll(reject);
}

RCT_EXPORT_METHOD(checkModelHealth : (RCTPromiseResolveBlock)resolve rejecter : (RCTPromiseRejectBlock)reject) {
  rejectAll(reject);
}

RCT_EXPORT_METHOD(predictFraud : (NSDictionary *)input resolver : (RCTPromiseResolveBlock)resolve rejecter : (RCTPromiseRejectBlock)reject) {
  rejectAll(reject);
}

RCT_EXPORT_METHOD(analyzeScam : (NSString *)text resolver : (RCTPromiseResolveBlock)resolve rejecter : (RCTPromiseRejectBlock)reject) {
  rejectAll(reject);
}

@end
