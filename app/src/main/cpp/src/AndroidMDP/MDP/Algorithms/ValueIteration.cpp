#include <AndroidMDP/MDP/Algorithms/ValueIteration.hpp>

namespace AndroidMDP
{
    namespace MDP
    {
        ValueIteration::ValueIteration(unsigned horizon, double epsilon, ValueFunction v) :
                horizon_(horizon), vParameter_(v)
        {
            setEpsilon(epsilon);
        }

        void ValueIteration::setEpsilon(double e)
        {
            epsilon_ = e;
        }
    }
}