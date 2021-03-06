package MIP;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

public class FacilityLocation {


	public static Solution calculate(String topologyName,double delayMatrix[][],int numberOfNodes,int delayConstraintSwitchController, int delayConstraintBetweenControllers, 
			double[] staticControllerCost, double[] controllerCapacity, int switchDemand)
	{
		Solution solution=null;
		try {

			//*****************************************
			IloCplex cplex = new IloCplex();

			//If there is a controller allocated at x[i]
			IloIntVar[][] x = new IloIntVar[staticControllerCost.length][numberOfNodes];
			
			//IloNumVar[] c =  cplex.numVarArray(numberOfNodes, 0., Double.MAX_VALUE);

			IloNumExpr objective = cplex.numExpr();

			for(int k=0;k<staticControllerCost.length;k++)
				for(int j=0;j<numberOfNodes;j++)
				{

					x[k][j]= cplex.boolVar();				
					objective=(cplex.sum(objective, cplex.prod(x[k][j], staticControllerCost[k])));
				}

			//if a switch i is assigned to a controller j
			IloIntVar[][] y = new IloIntVar[numberOfNodes][numberOfNodes];
			for(int i=0;i<numberOfNodes;i++)
				for(int j=0;j<numberOfNodes;j++)
					y[i][j]=cplex.boolVar();

			//Objective function
			cplex.addMinimize(objective);

			//*********************Constraints***********************************************

			
			//constraint 1 
			for(int j=0;j<numberOfNodes;j++)
			{
				IloNumExpr expr = cplex.numExpr();
				for(int k=0;k<staticControllerCost.length;k++)
				{
					expr = cplex.sum(expr,x[k][j]);
				}
				//Number of controllers allocated at j should be less or equal to 1
				cplex.addLe(expr,1);
			}
			
			
			//constraint 2
			for(int j=0;j<numberOfNodes;j++)
			{
				IloNumExpr expr = cplex.numExpr();
				for(int i=0;i<numberOfNodes;i++)
				{
					expr = cplex.sum(expr, y[i][j]);
				}
				//flows generated by switches
				expr=cplex.prod(expr, switchDemand);
				
				IloNumExpr expr_capacity = cplex.numExpr();
				
				for(int k=0;k<staticControllerCost.length;k++)
				{
					//controller j capacity 
					expr_capacity=cplex.sum(expr_capacity,cplex.prod(x[k][j], controllerCapacity[k]));
				}
				
				cplex.addLe(expr,expr_capacity);
				
			}
			
			//Constraint 3
			for(int i=0;i<numberOfNodes;i++)
			{
				IloNumExpr expr = cplex.numExpr();
				for(int j=0;j<numberOfNodes;j++)
				{
					expr = cplex.sum(expr, y[i][j]);
				}
				//Every switch should be connected to a controller and just one controller
				cplex.addEq(expr,1);
			}
			
			
			
		/*	//Cost definition
			for(int j=0;j<numberOfNodes;j++)
			{
				if(fixedCost)
				{
					cplex.addEq(c[j], costBase); //Value of each controller

					IloNumExpr expr = cplex.numExpr();
					for(int i=0;i<numberOfNodes;i++)
						expr = cplex.sum(expr, y[i][j]);
					cplex.addLe(expr,capacity);


				}
				else
				{


					IloNumExpr cost=cplex.sum(costBase,cplex.prod(costAdd, w[j]) );
					cplex.addEq(c[j],cost); //Value of each controller

					IloNumExpr expr = cplex.numExpr();
					for(int i=0;i<numberOfNodes;i++)
						expr = cplex.sum(expr, y[i][j]);
					cplex.addLe(expr, cplex.prod(cplex.sum(w[j],1),capacity));

					IloNumExpr cost=cplex.sum(costLow, cplex.prod(costHigh, cplex.sum(1,cplex.prod(-1, w[j]))));
					cplex.addEq(c[j],cost); //Value of each controller

					IloNumExpr expr = cplex.numExpr();
					for(int i=0;i<numberOfNodes;i++)
						expr = cplex.sum(expr, y[i][j]);
					cplex.addLe(expr, cplex.sum(capacityLowC,cplex.prod(numberOfNodes, cplex.sum(1,cplex.prod(-1, w[j])))));

				}
			}

			

			//Every switch should be connected to a controller and just one controller

			for(int i=0;i<numberOfNodes;i++)
			{
				IloNumExpr expr = cplex.numExpr();
				for(int j=0;j<numberOfNodes;j++)
					expr = cplex.sum(expr,y[i][j]);
				cplex.addEq(expr,1);
			}

			//One switch can be attached to a controller if and only if this controller was allocated
			for(int i=0;i<numberOfNodes;i++)
				for(int j=0;j<numberOfNodes;j++)
				{
					IloNumExpr expr = cplex.numExpr();
					expr = cplex.sum(y[i][j],cplex.prod(-1.0,x[j]));
					cplex.addLe(expr,0);
				}*/


			//constraint 04
			for(int i=0;i<numberOfNodes;i++)
			{
				for(int j=0;j<numberOfNodes;j++)
				{
					IloNumExpr expr = cplex.numExpr();
					expr = cplex.prod(y[i][j], delayMatrix[i][j]);
					//the delay between a switch and controller should be less than D
					cplex.addLe(expr,delayConstraintSwitchController);

				}
			}

			
			//constraint 05
			/*for(int i=0;i<numberOfNodes-1;i++)
			{
				IloNumExpr sumXi = cplex.numExpr();
				
				for(int k=0;k<staticControllerCost.length;k++)
					sumXi=cplex.sum(sumXi,x[k][i]);
					
				for(int j=i+1;j<numberOfNodes;j++)
				{
					IloNumExpr expr1 = cplex.numExpr();
					IloNumExpr expr2 = cplex.numExpr();
					IloNumExpr aux = cplex.numExpr();
					//System.out.println("x[i]"+x[i]+ " x[j]:"+x[j]+" delay:"+delayMatrix[i][j]);

					//   x[i] * Delay[i][j] <= D + (1-x[j])M

					// x[i] * Delay[i][j]
					
					IloNumExpr sumXj = cplex.numExpr();
					
					for(int k=0;k<staticControllerCost.length;k++)						
						sumXj=cplex.sum(sumXj,x[k][j]);
					
					
					expr1= cplex.prod(sumXi,delayMatrix[i][j]);
					
					// M * (1-x[j])
					aux=cplex.prod(20000, cplex.sum(1,cplex.prod(-1.0,sumXj)));
					// D + M*(1-x[j])
					expr2=cplex.sum(delayConstraintBetweenControllers, aux );

					//the delay between two controller should be less than Dc
					cplex.addLe(expr1,expr2);
				}
			}*/

			//Load balancing

			/*for(int i=0;i<numberOfNodes;i++)
				for(int j=i+1;j<numberOfNodes;j++)
				{
					IloNumExpr sum1 = cplex.numExpr();
					for(int k=0;k<numberOfNodes;k++)
						sum1 = cplex.sum(sum1,y[k][i]);

					IloNumExpr sum2 = cplex.numExpr();
					for(int k=0;k<numberOfNodes;k++)
						sum2 = cplex.sum(sum2,y[k][j]);

					IloNumExpr cap1 = cplex.numExpr();
					cap1=(cplex.sum(capacity,cplex.prod(w[i], capacity)));

					IloNumExpr cap2 = cplex.numExpr();
					cap2=(cplex.sum(capacity,cplex.prod(w[j], capacity)));


					IloNumExpr expr1 = cplex.numExpr();
					IloNumExpr expr2 = cplex.numExpr();

					expr1= cplex.diff(cplex.prod(sum1, cap1), cplex.prod(sum2, cap2));
					expr2= cplex.diff(cplex.prod(sum2, cap2), cplex.prod(sum1, cap1));

					IloNumExpr lessThen = cplex.numExpr();
					lessThen=cplex.prod(0.2, cplex.prod(cap1, cap2));


					cplex.addLe(expr1, lessThen);
					cplex.addLe(expr2, lessThen);
					//cplex.addLe(cplex.diff(cplex.prod(sum2, cap2), cplex.prod(sum1, cap1)), cplex.prod(0.2, cplex.prod(cap1, cap2)));
				}*/

			if ( cplex.solve() ) {
				//*******Solution variables**************************
				
				double totalCost=cplex.getObjValue();
				
				System.out.println("Cost:"+totalCost);
				
				int[][] switchToController=new int[numberOfNodes][numberOfNodes];
				int[][] controllerK_positionJ=new int[controllerCapacity.length][numberOfNodes];
				
				
				for(int i=0;i<numberOfNodes;i++)
					for(int j=0;j<numberOfNodes;j++)
					{
						switchToController[i][j]=(int) cplex.getValue(y[i][j]);
						//System.out.println("y["+i+"]["+j+"] "+cplex.getValue(y[i][j]));
					}
				
				
				for(int k=0;k<controllerCapacity.length;k++)
					for(int j=0;j<numberOfNodes;j++)
					{
						controllerK_positionJ[k][j]=(int) cplex.getValue(x[k][j]);
					}
				
				
				//***************************************************
				
			
				/*public Solution(String topologyName, double delayMax,
			int[][] controllerK_J, int[][] switchToController,
			double[] costPerController, double totalCost,
			int[] controllerCapacity, int switchFlowLoad, int delayMaxBetweenCOntrollers) {*/
					
				
				solution=new Solution(topologyName, (double)delayConstraintSwitchController, controllerK_positionJ, switchToController, staticControllerCost, totalCost, controllerCapacity, switchDemand, delayConstraintBetweenControllers);

			}
			cplex.end();
		}
		catch (IloException e) {
			System.out.println("Erro CPLEX");
			System.err.println("Concert exception �" + e + "� caught");
		}
		return solution;
	}
}
