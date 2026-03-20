import 'package:flutter/material.dart';
import 'package:fraudlens_flutter/fraudlens_flutter.dart';

void main() => runApp(const MyApp());

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'FraudLens Flutter',
      home: Scaffold(
        appBar: AppBar(title: const Text('FraudLens Flutter')),
        body: const _DemoBody(),
      ),
    );
  }
}

class _DemoBody extends StatefulWidget {
  const _DemoBody();

  @override
  State<_DemoBody> createState() => _DemoBodyState();
}

class _DemoBodyState extends State<_DemoBody> {
  String _log = 'Tap a button (Android only with SDK + Gradle wiring).';

  Future<void> _init() async {
    try {
      await FraudLensFlutter.initialize({
        'audioBaseUrl': '',
        'enableHttpLogging': true,
      });
      setState(() => _log = 'Initialized (add URLs/keys for real calls).');
    } catch (e) {
      setState(() => _log = 'Init error: $e');
    }
  }

  Future<void> _health() async {
    try {
      final h = await FraudLensFlutter.audioHealth();
      setState(() => _log = 'audioHealth: $h');
    } catch (e) {
      setState(() => _log = 'Error: $e');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(_log),
          const SizedBox(height: 16),
          FilledButton(onPressed: _init, child: const Text('Initialize (minimal)')),
          FilledButton(onPressed: _health, child: const Text('Audio health')),
        ],
      ),
    );
  }
}
