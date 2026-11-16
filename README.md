# DART-Core — 2-Stage Pipelined RISC-V (RV32E) Processor

![Language](https://img.shields.io/badge/HDL-Chisel%20(Scala)-red.svg)
![ISA](https://img.shields.io/badge/ISA-RISC--V%20RV32E-blue.svg)
![Target](https://img.shields.io/badge/Target-TinyTapeout%20%2F%20FPGA-success.svg)

**DART-Core** is a lightweight, synthesizable 32-bit RISC-V processor core written in **Chisel (Scala)**, implementing the **RV32E** base integer instruction set (16 general-purpose registers) with Control and Status Registers (**CSR**) support. 

Designed with a compact **2-stage pipeline** (`Fetch` / `Decode & Execute`), it targets area-constrained ASIC tapeouts (**TinyTapeout**) and low-footprint FPGA implementations.

---

## Key Features

* **ISA Compliance:** RISC-V `RV32E` base integer subset (16 × 32-bit registers `x0`–`x15` to minimize silicon area) + basic `Zicsr` support.
* **Microarchitecture:** 2-stage synchronous pipeline (`IF` → `ID/EX/WB`) with single-cycle register writeback.
* **Hazard Handling:** Control hazard mitigation via pipeline flushing/stalling on taken branches (`BEQ`, `BNE`, `BLT`, `BGE`, `BLTU`, `BGEU`) and jumps (`JAL`, `JALR`).
* **Modular RTL Design:** Clean separation between datapath, control decoding, CSR unit, and top-level Verilog generation (`GenTop.scala`).

---

## Microarchitecture

The core is partitioned into two main pipeline stages separated by synchronous pipeline registers (`PipelineReg.scala`):

```mermaid
flowchart LR
    subgraph Stage1["Stage 1: Fetch (IF)"]
        PC["Program Counter<br/>(program_counter.scala)"] --> IMEM["Instruction Memory<br/>Interface"]
        PC --> ADD4["+4 Adder"]
        ADD4 --> PCMUX{"Branch / Jump<br/>Mux"}
        PCMUX --> PC
    end

    Stage1 ==>|PipelineReg<br/>PC & Instruction| Stage2

    subgraph Stage2["Stage 2: Decode, Execute & Writeback (ID/EX)"]
        DEC["Instruction Decoder<br/>(Decode.scala)"]
        REG["Register File<br/>16x32-bit (RegFile.scala)"]
        ALU["ALU & Branch Unit<br/>(ALU.scala)"]
        CSR["CSR Unit<br/>(csr.scala)"]
        DMEM["Data Memory<br/>Interface"]

        DEC --> REG
        DEC --> ALU
        DEC --> CSR
        REG --> ALU
        ALU --> DMEM
        ALU --> REG
        DMEM --> REG
        CSR --> REG
    end

    ALU -.->|Target Address & Flush| PCMUX
