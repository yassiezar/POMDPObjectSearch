#include <AndroidMDP/MDP/Util.hpp>
#include <AndroidMDP/MDP/Policies/Policy.hpp>

namespace AndroidMDP
{
    namespace MDP
    {
        QFunction makeQFunction(const size_t S, const size_t A)
        {
            QFunction retVal = QFunction(S, A);
            retVal.fill(0.0);

            return retVal;
        }

        ValueFunction makeValueFunction(const size_t S)
        {
            Values retVal = Values(S);
            retVal.fill(0.0);

            return {retVal, Actions(S, 0)};
        }

        void bellmanOperatorInline(const QFunction &q, ValueFunction *v)
        {
            assert(v);
            auto &values = v->values;
            auto &actions = v->actions;

            for(size_t s = 0; s < actions.size(); s ++)
            {
                values(s) = q.row(s).maxCoeff(&actions[s]);
            }
        }
    }
}