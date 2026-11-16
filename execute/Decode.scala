package learn.core.execute
 
import chisel3._
import chisel3.util._
import learn.core.Constants._
import learn.core.CoreParams
 
class Decode(params: CoreParams) extends Module {
  val io = IO(new Bundle {
    val instr         = Input(UInt(params.nInstrBit.W))
 
    val alu_op        = Output(UInt(4.W))
    val reg_we        = Output(Bool())
    val csr_en        = Output(Bool())
    val imm           = Output(UInt(params.nDataBit.W))
 

    val sel_imm_src1  = Output(Bool())
    val sel_imm_src2  = Output(Bool())
 
    val mem_we        = Output(Bool())
    val mem_size      = Output(UInt(2.W))   
    val mem_unsigned  = Output(Bool())      
 
    val wb_sel        = Output(UInt(2.W))
  })
 

  val opcode = io.instr(6,  0)
  val funct3 = io.instr(14, 12)
  val funct7 = io.instr(31, 25)
 

  val immI = Cat(Fill(20, io.instr(31)), io.instr(31, 20))

  val immS = Cat(Fill(20, io.instr(31)), io.instr(31, 25), io.instr(11, 7))
 

  val immB = Cat(Fill(19, io.instr(31)), io.instr(31), io.instr(7),
                 io.instr(30, 25), io.instr(11, 8), 0.U(1.W))
 

  val immU = Cat(io.instr(31, 12), 0.U(12.W))
 

  val immJ = Cat(Fill(11, io.instr(31)), io.instr(19, 12),
                 io.instr(20), io.instr(30, 21), 0.U(1.W))
 

  io.imm          := 0.U
  io.alu_op       := ALU_ADD
  io.reg_we       := false.B
  io.sel_imm_src1 := false.B
  io.sel_imm_src2 := false.B
  io.mem_we       := false.B
  io.mem_size     := 2.U      
  io.mem_unsigned := false.B
  io.wb_sel       := 0.U
  io.csr_en       := false.B
 

  switch(opcode) {
 

    is (OPCODE_LUI) {
      io.imm          := immU
      io.alu_op       := ALU_ADD
      io.reg_we       := true.B
      io.sel_imm_src1 := false.B
      io.sel_imm_src2 := false.B
      io.wb_sel       := 3.U    
    }
 

    is (OPCODE_AUIPC) {
      io.imm          := immU
      io.alu_op       := ALU_ADD
      io.reg_we       := true.B
      io.sel_imm_src1 := true.B   
      io.sel_imm_src2 := true.B   
      io.wb_sel       := 0.U
    }
 
    is (OPCODE_LOAD) {
      io.imm          := immI
      io.alu_op       := ALU_ADD
      io.reg_we       := true.B
      io.sel_imm_src2 := true.B
      io.wb_sel       := 1.U

      switch(funct3) {
        is (FUNC3_LB)  { io.mem_size := 0.U; io.mem_unsigned := false.B }
        is (FUNC3_LH)  { io.mem_size := 1.U; io.mem_unsigned := false.B }
        is (FUNC3_LW)  { io.mem_size := 2.U; io.mem_unsigned := false.B }
        is (FUNC3_LBU) { io.mem_size := 0.U; io.mem_unsigned := true.B  }
        is (FUNC3_LHU) { io.mem_size := 1.U; io.mem_unsigned := true.B  }
      }
    }
 

    is (OPCODE_STORE) {
      io.imm          := immS
      io.alu_op       := ALU_ADD
      io.reg_we       := false.B
      io.sel_imm_src2 := true.B
      io.mem_we       := true.B
      switch(funct3) {
        is (FUNC3_SB) { io.mem_size := 0.U }
        is (FUNC3_SH) { io.mem_size := 1.U }
        is (FUNC3_SW) { io.mem_size := 2.U }
      }
    }
 

    is (OPCODE_OP_IMM) {
      io.imm          := immI
      io.reg_we       := true.B
      io.sel_imm_src2 := true.B
      io.wb_sel       := 0.U
      switch(funct3) {
        is (FUNC3_ADD)  { io.alu_op := ALU_ADD  }   
        is (FUNC3_OR)   { io.alu_op := ALU_OR   }   
        is (FUNC3_AND)  { io.alu_op := ALU_AND  }   
        is (FUNC3_XOR)  { io.alu_op := ALU_XOR  }  
        is (FUNC3_SLT)  { io.alu_op := ALU_SLT  }   
        is (FUNC3_SLTU) { io.alu_op := ALU_SLTU }   
        is (FUNC3_SLL)  { io.alu_op := ALU_SLL  }  
        is (FUNC3_SR) {                             
          switch(funct7) {
            is (FUNC7_SRL) { io.alu_op := ALU_SRL }
            is (FUNC7_SRA) { io.alu_op := ALU_SRA }
          }
        }
      }
    }
 
   
    is (OPCODE_OP) {
      io.reg_we       := true.B
      io.sel_imm_src2 := false.B
      io.wb_sel       := 0.U
      switch(funct7) {
       
        is (FUNC7_ADD) {
          switch(funct3) {
            is (F3_ADD_SUB) { io.alu_op := ALU_ADD  }   
            is (F3_SLL)     { io.alu_op := ALU_SLL  }   
            is (F3_SLT)     { io.alu_op := ALU_SLT  }   
            is (F3_SLTU)    { io.alu_op := ALU_SLTU }   
            is (F3_XOR)     { io.alu_op := ALU_XOR  }   
            is (F3_SRL)     { io.alu_op := ALU_SRL  }   
            is (F3_OR)      { io.alu_op := ALU_OR   }  
            is (F3_AND)     { io.alu_op := ALU_AND  }   
          }
        }

        is (FUNC7_SUB) {
          switch(funct3) {
            is (F3_ADD_SUB) { io.alu_op := ALU_SUB  }   
            is (F3_SRL)     { io.alu_op := ALU_SRA  }   
            is (F3_ADD8)    { io.alu_op := ALU_ADD8 }   
          }
        }
       
        is (F7_SMART) {
          switch(funct3) {
            is (F3_MIN) { io.alu_op := ALU_MIN }
            is (F3_MAX) { io.alu_op := ALU_MAX }
          }
        }
      }
    }
 

    is (OPCODE_BRANCH) {
      io.imm    := immB
      io.reg_we := false.B
      io.alu_op := ALU_SUB  
    } 
 

    is (OPCODE_JAL) {
      io.imm    := immJ
      io.reg_we := true.B
      io.alu_op := ALU_ADD
      io.wb_sel := 2.U    
    }
 

    is (OPCODE_JALR) {
      io.imm          := immI
      io.reg_we       := true.B
      io.sel_imm_src2 := true.B
      io.alu_op       := ALU_ADD
      io.wb_sel       := 2.U  
    }


    is(OPCODE_SYSTEM) {
    
      when(funct3 =/= 0.U) {
        io.csr_en := true.B
        io.reg_we := true.B  
        io.wb_sel :=  4.U
      }
  }
 

  }
}
