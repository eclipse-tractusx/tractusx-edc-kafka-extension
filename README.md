# Tractus-X EDC Kafka Extension

> **NOTE**: This is an experimental extension that is used to validate conceptual questions.
> It is not part of the official Eclipse Tractus-X connector offering.

## Overview

The Kafka Extension integrates Apache Kafka with the Eclipse Dataspace Connector to enable continuous, event-driven data
exchange in the Catena-X ecosystem.
It provides a secure and controlled way to stream data between organizations while maintaining data sovereignty.

## Key Features

- **Real-time Data Streaming**: Enable continuous data exchange using Kafka's publish/subscribe model
- **Secure Access Control**: Dynamic credential provisioning and token-based authentication
- **EDC Integration**: Seamless integration with the Eclipse Dataspace Connector framework
- **Data Sovereignty**: Maintain control over data sharing with policy-based access control
- **Standardized Protocol**: Leverage the widely-adopted Kafka protocol for data streaming

## Use Cases

This extension is particularly valuable for use cases requiring real-time data exchange, such as:

- **Quality Management**: Predictive maintenance and early warning notification
- **Demand & Capacity Management**: Real-time planning and coordination
- **Digital Twin / Asset Administration Shell**: Real-time operational monitoring and shopfloor efficiency
- **Traceability**: Event-driven tracking of part movements
- **Circular Economy / Product Pass**: Lifecycle event streaming
- **ESG-Monitoring**: Real-time environmental data collection

## Project Structure

The project is organized into the following main directories:

- **poc**: Contains the Proof of Concept implementation
    - `kafka-pull`: Core extension modules for Kafka integration
    - `runtimes`: Example implementations and test setups

- **docs**: Documentation
    - `architecture`: Architecture and design documents
    - `administration`: Administration documentation

## Documentation

### Getting Started

- [poc README](poc/README.md): Instructions for running the Proof of Concept implementation
- [Documentation Overview](docs/README.md): Central hub for all documentation

### Administration

- [Admin Manual](docs/administration/admin-manual.md): How to set up, configure, and maintain the Kafka extension

### Technical Documentation

- [Solution Design](docs/architecture/solution-design-kafka-pull.md):
  Detailed architecture and design of the Kafka extension
- [Kafka Broker Extension](poc/kafka-pull/README.md): Technical details of the extension implementation

## Quick Start

To quickly try out the Kafka extension:

1. Clone this repository
2. Follow the [installation instructions](INSTALL.md) to run the example
3. Use the provided Bruno collection to test the data exchange workflow

## Changelog

See the [Changelog](CHANGELOG.md) for details about the changes in each release.

## Contributing

See [CONTRIBUTING](CONTRIBUTING.md) for details on how to contribute to this project.

## Licenses

For used licenses, please see the [NOTICE](NOTICE.md).
