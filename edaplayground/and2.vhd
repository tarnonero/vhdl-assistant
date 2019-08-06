entity and2 is
	port(in0, in1: in bit;
    	 out0: out bit);
end and2;

architecture behav of and2 is
	begin
    	out0 <= in0 and in1;
end behav;