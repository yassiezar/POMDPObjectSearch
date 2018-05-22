#include <MDP/model.hpp>

namespace MDPNameSpace
{
    Model::Model() {}

    double Model::getExpectedReward(size_t s, size_t a, size_t s1) const
    {
        return getReward(decodeState(s), a, decodeState(s1));
    }

    double Model::getReward(StateVector s, size_t a, StateVector s1) const
    {
        if(s1[S_OBSERVATION] == target) return 100;
        return -5.0;
    }

    double Model::getTransitionProbability(size_t s, size_t a, size_t s1) const
    {
        return getTransition(decodeState(s), a, decodeState(s1));
    }

        double Model::getTransition(const StateVector s1, size_t a, const StateVector s2) const
        {
            // Calculate angular distance travelled
            int panDistance = s1[S_AGENT_PAN] - s2[S_AGENT_PAN];
            int tiltDistance = s1[S_AGENT_TILT] - s2[S_AGENT_TILT];

            // Can only move one grid block at a time
            if(std::abs(panDistance) + std::abs(tiltDistance) > blockSize)
            {
                return 0.0;
            }

            if(s1[S_OBSERVATION] == target)
            {
                if(s1 == s2) return 1.0;
                return 0.0;
            }

            // Agent can only move in direction specified by action
            if(a == A_UP && tiltDistance != blockSize && s1[S_AGENT_TILT] != 0) return 0.0;
            if(a == A_DOWN && tiltDistance != -blockSize && s1[S_AGENT_TILT] != gridSize-1) return 0.0;
            if(a == A_LEFT && panDistance != -blockSize && s1[S_AGENT_PAN] != gridSize-1) return 0.0;
            if(a == A_RIGHT && panDistance != blockSize && s1[S_AGENT_PAN] != 0) return 0.0;


            // Agent goes in opposite direction when hitting border. Priority on pan
            if(s1[S_AGENT_PAN] == 0)
            {
                if(a == A_RIGHT && s1 == s2)
                {
                    return 1.0;
                }
                else if(a == A_RIGHT) return 0.0;
            }
            if(s1[S_AGENT_PAN] == gridSize - 1)
            {
                if(a == A_LEFT && s1 == s2)
                {
                    return 1.0;
                }
                else if(a == A_LEFT) return 0.0;
            }
            if(s1[S_AGENT_TILT] == 0)
            {
                if(a == A_UP && s1 == s2)
                {
                    return 1.0;
                }
                else if(a == A_UP) return 0.0;
            }
            if(s1[S_AGENT_TILT] == gridSize - 1)
            {
                if(a == A_DOWN && s1 == s2)
                {
                    return 1.0;
                }
                else if(a == A_DOWN) return 0.0;
            }

            // Agent must move and cannot look back to previous location, unless at border
            if(panDistance == 0 && tiltDistance == 0) return 0.0;

            return (double)(observationData[objects[s1[S_OBSERVATION]]][actions[a]][objects[s2[S_OBSERVATION]]]);
        }


    StateVector Model::decodeState(size_t s) const
    {
        StateVector vector;

        vector[S_AGENT_PAN] = s % gridSize;
        s /= gridSize;
        vector[S_AGENT_TILT] = s % gridSize;
        vector[S_OBSERVATION] = s / gridSize;

        return vector;
    }

    void Model::setTarget(const size_t t)
    {
        target = t;
    }
}