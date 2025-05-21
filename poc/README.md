# Tractus-X EDC Kafka Extension - Proof of Concept

## Overview

This Proof of Concept (PoC) demonstrates the integration of Apache Kafka with the Eclipse Dataspace Connector (EDC) to
enable real-time data streaming between data providers and consumers in a secure and controlled manner. 
The PoC showcases how Kafka can be used as a streaming protocol within the Tractus-X ecosystem.

## Project Structure

The project is organized into the following main directories:

- **kafka-pull**: Contains the core extension modules that enable Kafka integration with EDC
    - `data-address-kafka`: Data address implementation for Kafka
    - `kafka-broker-extension`: Control Plane extension for managing Kafka broker access
    - `validator-data-address-kafka`: Validator for Kafka data addresses
    - `collections`: Bruno collection with HTTP requests for testing

- **runtimes**: Contains example implementations and test setups
    - `edc`: EDC control plane and data plane configurations
    - `kafka`: Kafka server, producer, and consumer examples
    - `oauth`: OAuth service for authentication
