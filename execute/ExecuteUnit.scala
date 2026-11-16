package learn.core.execute

import chisel3._
import chisel3.util._
import learn.core.Constants._
import learn.core.CoreParams

class ExecuteUnit(params: CoreParams) extends Module {
  val io = IO(new Bundle {
    val instr        = Input(UInt(params.nInstrBit.W))
    val pc_plus_one  = Input(UInt(params.nAddrBit.W))
    val pc_current   = Input(UInt(params.nAddrBit.W))

    val d_addr  = Output(UInt(params.nDataBit.W))   
    val d_wdata = Output(UInt(params.nDataBit.W))  
    val d_we    = Output(Bool())
    val d_be    = Output(UInt(4.W))                
    val d_rdata = Input(UInt(params.nDataBit.W))

    val branchTaken  = Output(Bool())
    val branchTarget = Output(UInt(params.nAddrBit.W))
    val flush        = Output(Bool())
    val stall        = Output(Bool())
  })

  val decode  = Module(new Decode(params))
  val regFile = Module(new RegFile(params))
  val alu     = Module(new ALU(params))
  val csr = Module(new CSR(params))

  csr.io.csr_addr := io.instr(31, 20)
  decode.io.instr := io.instr

  csr.io.csr_en := decode.io.csr_en

  regFile.io.rs1_addr := io.instr(19, 15)
  regFile.io.rs2_addr := io.instr(24, 20)
  regFile.io.rd_addr  := io.instr(11,  7)

  val current_pc_word = io.pc_current              
  val current_pc_byte = Cat(current_pc_word, 0.U(2.W))     

  val opcode = io.instr(6, 0)
  val funct3 = io.instr(14, 12)

  val is_calc_target_phase = RegInit(false.B)
  val branch_taken_saved   = RegInit(false.B)     

  // ==========================================
  // ROUTAGE ALU 100% UNIFIÉ (JAL + BRANCH Cycle 2)
  // ==========================================
  val force_alu_target = is_calc_target_phase || (opcode === OP_JAL)

  alu.io.src1 := Mux(force_alu_target, current_pc_byte, 
                     Mux(decode.io.sel_imm_src1, current_pc_byte, regFile.io.rs1_data))
                     
  alu.io.src2 := Mux(force_alu_target, decode.io.imm, 
                     Mux(decode.io.sel_imm_src2, decode.io.imm, regFile.io.rs2_data))
                     
  alu.io.alu_op := Mux(force_alu_target, ALU_ADD, decode.io.alu_op)

  io.stall := false.B

  io.d_addr := alu.io.res
  io.d_we   := decode.io.mem_we

  val byte_offset = alu.io.res(1, 0)
  val byte_en = UIntToOH(byte_offset) 
  val half_en = Mux(byte_offset(1), "b1100".U(4.W), "b0011".U(4.W))

  io.d_be := MuxLookup(decode.io.mem_size, "b1111".U(4.W))(Seq(
    0.U -> byte_en,
    1.U -> half_en,
    2.U -> "b1111".U(4.W)
  ))

  val rs2_d = regFile.io.rs2_data
  io.d_wdata := MuxLookup(decode.io.mem_size, rs2_d)(Seq(
    0.U -> Fill(4, rs2_d(7, 0)),
    1.U -> Fill(2, rs2_d(15, 0)),
    2.U -> rs2_d
  ))

  val load_byte_raw = MuxLookup(byte_offset, io.d_rdata(7, 0))(Seq(
    0.U -> io.d_rdata( 7,  0),
    1.U -> io.d_rdata(15,  8),
    2.U -> io.d_rdata(23, 16),
    3.U -> io.d_rdata(31, 24)
  ))
  val load_half_raw = Mux(byte_offset(1), io.d_rdata(31, 16), io.d_rdata(15, 0))

  val load_byte = Mux(decode.io.mem_unsigned,
    Cat(0.U(24.W), load_byte_raw),
    Cat(Fill(24, load_byte_raw(7)), load_byte_raw))

  val load_half = Mux(decode.io.mem_unsigned,
    Cat(0.U(16.W), load_half_raw),
    Cat(Fill(16, load_half_raw(15)), load_half_raw))

  val load_data = MuxLookup(decode.io.mem_size, io.d_rdata)(Seq(
    0.U -> load_byte,
    1.U -> load_half,
    2.U -> io.d_rdata
  ))

  val ret_addr = Cat(io.pc_plus_one, 0.U(2.W)) 
  
  val wb_data = MuxLookup(decode.io.wb_sel, alu.io.res)(Seq(
    0.U -> alu.io.res,            
    1.U -> load_data,             
    2.U -> ret_addr,             
    3.U -> decode.io.imm,
    4.U -> csr.io.csr_rdata        
  ))

  regFile.io.wen     := decode.io.reg_we
  regFile.io.rd_data := wb_data

  val rs1 = regFile.io.rs1_data
  val rs2 = regFile.io.rs2_data

  val cmp_diff = alu.io.res 
  val isEq = (cmp_diff === 0.U) 
  
  val sign1 = rs1(params.nDataBit - 1)
  val sign2 = rs2(params.nDataBit - 1)
  val diff_sign = cmp_diff(params.nDataBit - 1)

  val isLT  = Mux(sign1 === sign2, diff_sign, sign1)
  val isLTU = Mux(sign1 === sign2, diff_sign, sign2)

  io.branchTaken  := false.B
  io.branchTarget := 0.U
  io.flush        := false.B


  switch(opcode) {
    is (OP_JAL) {
      io.branchTaken  := true.B
      io.flush        := true.B
      io.branchTarget := alu.io.res(params.nAddrBit + 1, 2)
    }

    is (OP_JALR) {
      val jalr_byte_target = alu.io.res & ~1.U(params.nDataBit.W)   
      io.branchTaken  := true.B
      io.branchTarget := jalr_byte_target(params.nAddrBit + 1, 2)
      io.flush        := true.B
    }
    
    is (OP_BRANCH) {
      when(!is_calc_target_phase) {
         
         val taken = MuxLookup(funct3, false.B)(Seq(
            FUNC3_BEQ  -> isEq,
            FUNC3_BNE  -> !isEq,
            FUNC3_BLT  -> isLT,
            FUNC3_BGE  -> !isLT,
            FUNC3_BLTU -> isLTU,
            FUNC3_BGEU -> !isLTU
          ))
          
          when(taken) {
              io.stall := true.B
              is_calc_target_phase := true.B
              branch_taken_saved := true.B
          }
      } .otherwise {
        
          io.branchTaken  := true.B
          io.branchTarget := alu.io.res(params.nAddrBit + 1, 2)
          io.flush        := true.B
          
          is_calc_target_phase := false.B
          branch_taken_saved := false.B
      }
    }
  }
}